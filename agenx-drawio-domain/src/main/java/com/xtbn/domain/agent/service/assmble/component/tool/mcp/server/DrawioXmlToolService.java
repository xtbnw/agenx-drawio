package com.xtbn.domain.agent.service.assmble.component.tool.mcp.server;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xtbn.domain.agent.service.assmble.component.plugin.support.DrawioLayoutValidationSupport;
import com.xtbn.domain.agent.service.assmble.component.plugin.support.DrawioXmlValidationSupport;
import com.xtbn.types.enums.DrawioXmlValidationErrorCode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class DrawioXmlToolService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final DrawioXmlValidationSupport formatValidationSupport;
    private final DrawioLayoutValidationSupport layoutValidationSupport;

    public DrawioXmlToolService(DrawioXmlValidationSupport formatValidationSupport,
                                DrawioLayoutValidationSupport layoutValidationSupport) {
        this.formatValidationSupport = formatValidationSupport;
        this.layoutValidationSupport = layoutValidationSupport;
    }

    @Tool(description = "Render a draw.io diagram as importable XML. Use this tool whenever the final answer must be draw.io XML.")
    public String renderDrawioXml(RenderRequest request) {
        List<NodeSpec> nodes = request.getNodes() == null ? new ArrayList<>() : request.getNodes();
        List<EdgeSpec> edges = request.getEdges() == null ? new ArrayList<>() : request.getEdges();

        String pageName = isBlank(request.getPageName()) ? "Page-1" : request.getPageName();
        int pageWidth = request.getPageWidth() == null ? 1600 : Math.max(request.getPageWidth(), 800);
        int pageHeight = request.getPageHeight() == null ? 1200 : Math.max(request.getPageHeight(), 600);

        StringBuilder xml = new StringBuilder(4096);
        xml.append("<mxfile host=\"app.diagrams.net\" modified=\"")
                .append(escapeXml(Instant.now().toString()))
                .append("\" agent=\"drawioXmlAgent\" version=\"26.0.0\">");
        xml.append("<diagram id=\"page-1\" name=\"")
                .append(escapeXml(pageName))
                .append("\">");
        xml.append("<mxGraphModel dx=\"")
                .append(pageWidth)
                .append("\" dy=\"")
                .append(pageHeight)
                .append("\" grid=\"1\" gridSize=\"10\" guides=\"1\" tooltips=\"1\" connect=\"1\" arrows=\"1\" fold=\"1\" page=\"1\" pageScale=\"1\" pageWidth=\"")
                .append(pageWidth)
                .append("\" pageHeight=\"")
                .append(pageHeight)
                .append("\" math=\"0\" shadow=\"0\">");
        xml.append("<root>");
        xml.append("<mxCell id=\"0\"/>");
        xml.append("<mxCell id=\"1\" parent=\"0\"/>");

        nodes.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(node -> isContainer(node.getType()) ? 0 : 1))
                .forEach(node -> appendNode(xml, node));

        edges.stream()
                .filter(Objects::nonNull)
                .forEach(edge -> appendEdge(xml, edge));

        xml.append("</root></mxGraphModel></diagram></mxfile>");
        return xml.toString();
    }

    @Tool(description = "Analyze draw.io XML and return a conservative diagnostic report. Use this before revising XML in polish or max mode. Prefer profile=polish for softer review and profile=max for stricter review.")
    public String analyzeDrawioXml(AnalyzeRequest request) {
        String xml = request == null ? null : request.getXml();
        String profile = normalizeProfile(request == null ? null : request.getProfile());

        AnalysisReport report = new AnalysisReport();
        report.setProfile(profile);
        report.setStatus("PASS");

        DrawioXmlValidationSupport.ValidationResult formatResult = formatValidationSupport.validate(xml);
        if (!formatResult.isValid()) {
            report.getFormatIssues().add(toIssue(formatResult, "high", "Fix the XML structure before doing any layout optimization."));
            report.setStatus("REVISE");
        }

        DrawioXmlValidationSupport.ValidationResult layoutResult = layoutValidationSupport.validate(xml);
        if (layoutResult.isValid()) {
            report.setSummary(buildSummary(report));
            return writeReport(report);
        }

        AnalysisIssue layoutIssue = toIssue(layoutResult, severityFor(layoutResult.getErrorCode(), profile), suggestionFor(layoutResult.getErrorCode()));
        if (shouldIncludeLayoutIssue(layoutIssue, profile)) {
            report.getLayoutIssues().add(layoutIssue);
            report.setStatus("REVISE");
        }

        report.setSummary(buildSummary(report));
        return writeReport(report);
    }

    private void appendNode(StringBuilder xml, NodeSpec node) {
        String id = safeId(node.getId(), "node");
        String parentId = isBlank(node.getParentId()) ? "1" : escapeXml(node.getParentId());
        int x = node.getX() == null ? 0 : node.getX();
        int y = node.getY() == null ? 0 : node.getY();
        int width = node.getWidth() == null ? defaultWidth(node.getType()) : Math.max(node.getWidth(), 40);
        int height = node.getHeight() == null ? defaultHeight(node.getType()) : Math.max(node.getHeight(), 30);
        String style = isBlank(node.getStyle()) ? defaultNodeStyle(node.getType()) : node.getStyle();
        String value = escapeXml(nullToEmpty(node.getLabel()));

        xml.append("<mxCell id=\"")
                .append(id)
                .append("\" value=\"")
                .append(value)
                .append("\" style=\"")
                .append(escapeXml(style))
                .append("\" vertex=\"1\" parent=\"")
                .append(parentId)
                .append("\">");
        xml.append("<mxGeometry x=\"")
                .append(x)
                .append("\" y=\"")
                .append(y)
                .append("\" width=\"")
                .append(width)
                .append("\" height=\"")
                .append(height)
                .append("\" as=\"geometry\"/>");
        xml.append("</mxCell>");
    }

    private void appendEdge(StringBuilder xml, EdgeSpec edge) {
        String id = safeId(edge.getId(), "edge");
        String style = buildEdgeStyle(edge);
        String value = escapeXml(nullToEmpty(edge.getLabel()));
        String sourceId = escapeXml(nullToEmpty(edge.getSourceId()));
        String targetId = escapeXml(nullToEmpty(edge.getTargetId()));
        String parentId = escapeXml(isBlank(edge.getParentId()) ? "1" : edge.getParentId());

        xml.append("<mxCell id=\"")
                .append(id)
                .append("\" value=\"")
                .append(value)
                .append("\" style=\"")
                .append(escapeXml(style))
                .append("\" edge=\"1\" parent=\"")
                .append(parentId)
                .append("\"");

        if (!isBlank(sourceId)) {
            xml.append(" source=\"").append(sourceId).append("\"");
        }
        if (!isBlank(targetId)) {
            xml.append(" target=\"").append(targetId).append("\"");
        }

        xml.append(">");
        appendEdgeGeometry(xml, edge);
        xml.append("</mxCell>");
    }

    private void appendEdgeGeometry(StringBuilder xml, EdgeSpec edge) {
        boolean hasPoints = edge.getSourcePoint() != null
                || edge.getTargetPoint() != null
                || (edge.getWaypoints() != null && !edge.getWaypoints().isEmpty());
        if (!hasPoints) {
            xml.append("<mxGeometry relative=\"1\" as=\"geometry\"/>");
            return;
        }

        xml.append("<mxGeometry relative=\"1\" as=\"geometry\">");
        appendPoint(xml, "sourcePoint", edge.getSourcePoint());
        appendPoint(xml, "targetPoint", edge.getTargetPoint());
        if (edge.getWaypoints() != null && !edge.getWaypoints().isEmpty()) {
            xml.append("<Array as=\"points\">");
            for (PointSpec waypoint : edge.getWaypoints()) {
                appendPoint(xml, null, waypoint);
            }
            xml.append("</Array>");
        }
        xml.append("</mxGeometry>");
    }

    private void appendPoint(StringBuilder xml, String asName, PointSpec point) {
        if (point == null || point.getX() == null || point.getY() == null) {
            return;
        }
        xml.append("<mxPoint x=\"")
                .append(point.getX())
                .append("\" y=\"")
                .append(point.getY())
                .append("\"");
        if (!isBlank(asName)) {
            xml.append(" as=\"").append(asName).append("\"");
        }
        xml.append("/>");
    }

    private String buildEdgeStyle(EdgeSpec edge) {
        String baseStyle = isBlank(edge.getStyle()) ? defaultEdgeStyle() : edge.getStyle();
        StringBuilder style = new StringBuilder(baseStyle);
        appendStyle(style, "dashed", edge.getDashed());
        appendStyle(style, "exitX", edge.getExitX());
        appendStyle(style, "exitY", edge.getExitY());
        appendStyle(style, "entryX", edge.getEntryX());
        appendStyle(style, "entryY", edge.getEntryY());
        return style.toString();
    }

    private void appendStyle(StringBuilder style, String key, Object value) {
        if (value == null) {
            return;
        }
        if (style.length() > 0 && style.charAt(style.length() - 1) != ';') {
            style.append(';');
        }
        style.append(key).append('=').append(value).append(';');
    }

    private boolean isContainer(String type) {
        return "container".equalsIgnoreCase(nullToEmpty(type)) || "swimlane".equalsIgnoreCase(nullToEmpty(type));
    }

    private int defaultWidth(String type) {
        String normalized = nullToEmpty(type).toLowerCase();
        if ("decision".equals(normalized)) return 140;
        if (isContainer(normalized)) return 320;
        if ("note".equals(normalized)) return 180;
        return 160;
    }

    private int defaultHeight(String type) {
        String normalized = nullToEmpty(type).toLowerCase();
        if ("decision".equals(normalized)) return 100;
        if (isContainer(normalized)) return 220;
        if ("database".equals(normalized)) return 80;
        return 60;
    }

    private String defaultNodeStyle(String type) {
        String normalized = nullToEmpty(type).toLowerCase();
        return switch (normalized) {
            case "start", "end", "terminator" -> "ellipse;whiteSpace=wrap;html=1;aspect=fixed;";
            case "decision" -> "rhombus;whiteSpace=wrap;html=1;";
            case "database", "datastore" -> "shape=cylinder;whiteSpace=wrap;html=1;boundedLbl=1;";
            case "note" -> "shape=note;whiteSpace=wrap;html=1;";
            case "container", "swimlane" -> "swimlane;whiteSpace=wrap;html=1;startSize=30;";
            case "actor" -> "shape=umlActor;verticalLabelPosition=bottom;verticalAlign=top;html=1;";
            default -> "rounded=1;whiteSpace=wrap;html=1;";
        };
    }

    private String defaultEdgeStyle() {
        return "edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;endArrow=block;endFill=1;";
    }

    private String normalizeProfile(String profile) {
        if ("max".equalsIgnoreCase(nullToEmpty(profile))) {
            return "max";
        }
        return "polish";
    }

    private AnalysisIssue toIssue(DrawioXmlValidationSupport.ValidationResult result, String severity, String suggestion) {
        AnalysisIssue issue = new AnalysisIssue();
        issue.setCode(result.getErrorCode().name());
        issue.setSeverity(severity);
        issue.setMessage(result.getErrorMessage());
        issue.setSuggestion(suggestion);
        return issue;
    }

    private boolean shouldIncludeLayoutIssue(AnalysisIssue issue, String profile) {
        if ("max".equals(profile)) {
            return true;
        }
        return !"low".equals(issue.getSeverity());
    }

    private String severityFor(DrawioXmlValidationErrorCode errorCode, String profile) {
        return switch (errorCode) {
            case CONTAINER_OVERLAP, CONTAINER_CHILD_OUT_OF_BOUNDS -> "high";
            case SEQUENCE_MESSAGE_BAND_CONFLICT -> "max".equals(profile) ? "high" : "medium";
            case EDGE_OVERLAP_RISK, USECASE_EDGE_OVERLAP -> "max".equals(profile) ? "medium" : "low";
            default -> "medium";
        };
    }

    private String suggestionFor(DrawioXmlValidationErrorCode errorCode) {
        return switch (errorCode) {
            case CONTAINER_OVERLAP -> "Separate the containers and preserve visible whitespace between group boundaries.";
            case CONTAINER_CHILD_OUT_OF_BOUNDS -> "Resize or reposition the container so every child fits inside with padding and routing space.";
            case EDGE_OVERLAP_RISK -> "Separate repeated or parallel edges with distinct exit sides, elbows, or waypoints.";
            case SEQUENCE_MESSAGE_BAND_CONFLICT -> "Assign each message or return its own y-band instead of reusing the same line segment.";
            case USECASE_EDGE_OVERLAP -> "Fan out actor-to-use-case edges with distinct routing so they do not visually merge.";
            default -> "Revise the XML conservatively and keep the diagram semantics unchanged.";
        };
    }

    private AnalysisSummary buildSummary(AnalysisReport report) {
        AnalysisSummary summary = new AnalysisSummary();
        summary.setFormatIssueCount(report.getFormatIssues().size());
        summary.setLayoutIssueCount(report.getLayoutIssues().size());
        summary.setBlockingIssueCount((int) report.getAllIssues().stream()
                .filter(issue -> "high".equals(issue.getSeverity()))
                .count());
        return summary;
    }

    private String writeReport(AnalysisReport report) {
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(report);
        } catch (Exception e) {
            log.warn("Failed to serialize draw.io analysis report", e);
            return "{\"status\":\"REVISE\",\"summary\":{\"formatIssueCount\":1,\"layoutIssueCount\":0,\"blockingIssueCount\":1}}";
        }
    }

    private String safeId(String id, String prefix) {
        return escapeXml(isBlank(id) ? prefix + "-" + System.nanoTime() : id);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String escapeXml(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RenderRequest {
        @JsonProperty("pageName")
        @JsonPropertyDescription("Diagram page name, for example OrderFlow or SystemArchitecture.")
        private String pageName;

        @JsonProperty("pageWidth")
        @JsonPropertyDescription("Page width in pixels, for example 1600.")
        private Integer pageWidth;

        @JsonProperty("pageHeight")
        @JsonPropertyDescription("Page height in pixels, for example 1200.")
        private Integer pageHeight;

        @JsonProperty("nodes")
        @JsonPropertyDescription("Nodes to render. Every node must have a unique id and label. Use type values like start, end, process, decision, database, note, container, swimlane, actor.")
        private List<NodeSpec> nodes;

        @JsonProperty("edges")
        @JsonPropertyDescription("Directed connections between nodes. Provide sourceId and targetId, and optional label.")
        private List<EdgeSpec> edges;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AnalyzeRequest {
        @JsonProperty("xml")
        @JsonPropertyDescription("The draw.io XML to inspect.")
        private String xml;

        @JsonProperty("profile")
        @JsonPropertyDescription("Review profile: polish for conservative feedback, max for stricter feedback.")
        private String profile;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class NodeSpec {
        @JsonProperty("id")
        @JsonPropertyDescription("Unique node id, for example order_service or payment_gateway.")
        private String id;

        @JsonProperty("label")
        @JsonPropertyDescription("Text shown in the node.")
        private String label;

        @JsonProperty("type")
        @JsonPropertyDescription("Node type: start, end, process, decision, database, note, container, swimlane, actor.")
        private String type;

        @JsonProperty("parentId")
        @JsonPropertyDescription("Optional parent container id. Omit for top-level nodes.")
        private String parentId;

        @JsonProperty("x")
        @JsonPropertyDescription("X coordinate.")
        private Integer x;

        @JsonProperty("y")
        @JsonPropertyDescription("Y coordinate.")
        private Integer y;

        @JsonProperty("width")
        @JsonPropertyDescription("Node width.")
        private Integer width;

        @JsonProperty("height")
        @JsonPropertyDescription("Node height.")
        private Integer height;

        @JsonProperty("style")
        @JsonPropertyDescription("Optional raw draw.io style string. Omit to use a sensible default by type.")
        private String style;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class EdgeSpec {
        @JsonProperty("id")
        @JsonPropertyDescription("Unique edge id.")
        private String id;

        @JsonProperty("label")
        @JsonPropertyDescription("Optional edge label such as yes, no, REST, async event.")
        private String label;

        @JsonProperty("sourceId")
        @JsonPropertyDescription("Source node id.")
        private String sourceId;

        @JsonProperty("targetId")
        @JsonPropertyDescription("Target node id.")
        private String targetId;

        @JsonProperty("parentId")
        @JsonPropertyDescription("Optional parent cell id for grouped edges. Omit for the default root cell.")
        private String parentId;

        @JsonProperty("sourcePoint")
        @JsonPropertyDescription("Optional explicit source point with x and y for detached or precisely routed edges.")
        private PointSpec sourcePoint;

        @JsonProperty("targetPoint")
        @JsonPropertyDescription("Optional explicit target point with x and y for detached or precisely routed edges.")
        private PointSpec targetPoint;

        @JsonProperty("waypoints")
        @JsonPropertyDescription("Optional intermediate routing waypoints. Use this to separate parallel or repeated edges.")
        private List<PointSpec> waypoints;

        @JsonProperty("entryX")
        @JsonPropertyDescription("Optional normalized entry X ratio between 0 and 1.")
        private Double entryX;

        @JsonProperty("entryY")
        @JsonPropertyDescription("Optional normalized entry Y ratio between 0 and 1.")
        private Double entryY;

        @JsonProperty("exitX")
        @JsonPropertyDescription("Optional normalized exit X ratio between 0 and 1.")
        private Double exitX;

        @JsonProperty("exitY")
        @JsonPropertyDescription("Optional normalized exit Y ratio between 0 and 1.")
        private Double exitY;

        @JsonProperty("dashed")
        @JsonPropertyDescription("Optional dashed flag for async or return edges.")
        private Boolean dashed;

        @JsonProperty("style")
        @JsonPropertyDescription("Optional raw draw.io edge style string.")
        private String style;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PointSpec {
        @JsonProperty("x")
        @JsonPropertyDescription("Point x coordinate.")
        private Integer x;

        @JsonProperty("y")
        @JsonPropertyDescription("Point y coordinate.")
        private Integer y;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AnalysisReport {
        @JsonProperty("status")
        private String status;

        @JsonProperty("profile")
        private String profile;

        @JsonProperty("formatIssues")
        private List<AnalysisIssue> formatIssues = new ArrayList<>();

        @JsonProperty("layoutIssues")
        private List<AnalysisIssue> layoutIssues = new ArrayList<>();

        @JsonProperty("summary")
        private AnalysisSummary summary;

        public List<AnalysisIssue> getAllIssues() {
            List<AnalysisIssue> issues = new ArrayList<>(formatIssues);
            issues.addAll(layoutIssues);
            return issues;
        }
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AnalysisIssue {
        @JsonProperty("code")
        private String code;

        @JsonProperty("severity")
        private String severity;

        @JsonProperty("message")
        private String message;

        @JsonProperty("suggestion")
        private String suggestion;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AnalysisSummary {
        @JsonProperty("formatIssueCount")
        private Integer formatIssueCount;

        @JsonProperty("layoutIssueCount")
        private Integer layoutIssueCount;

        @JsonProperty("blockingIssueCount")
        private Integer blockingIssueCount;
    }
}
