package com.xtbn.domain.agent.service.assmble.component.tool.mcp.server;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
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
        String style = isBlank(edge.getStyle()) ? defaultEdgeStyle() : edge.getStyle();
        String value = escapeXml(nullToEmpty(edge.getLabel()));
        String sourceId = escapeXml(nullToEmpty(edge.getSourceId()));
        String targetId = escapeXml(nullToEmpty(edge.getTargetId()));

        xml.append("<mxCell id=\"")
                .append(id)
                .append("\" value=\"")
                .append(value)
                .append("\" style=\"")
                .append(escapeXml(style))
                .append("\" edge=\"1\" parent=\"1\"");

        if (!isBlank(sourceId)) {
            xml.append(" source=\"").append(sourceId).append("\"");
        }
        if (!isBlank(targetId)) {
            xml.append(" target=\"").append(targetId).append("\"");
        }

        xml.append(">");
        xml.append("<mxGeometry relative=\"1\" as=\"geometry\"/>");
        xml.append("</mxCell>");
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

        @JsonProperty("style")
        @JsonPropertyDescription("Optional raw draw.io edge style string.")
        private String style;
    }
}
