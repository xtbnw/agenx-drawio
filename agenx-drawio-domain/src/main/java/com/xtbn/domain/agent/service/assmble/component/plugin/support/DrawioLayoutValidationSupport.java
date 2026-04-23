package com.xtbn.domain.agent.service.assmble.component.plugin.support;

import com.xtbn.types.enums.DrawioXmlValidationErrorCode;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class DrawioLayoutValidationSupport {
    private final DrawioXmlParsingSupport parsingSupport;

    public DrawioLayoutValidationSupport(DrawioXmlParsingSupport parsingSupport) {
        this.parsingSupport = parsingSupport;
    }

    public DrawioXmlValidationSupport.ValidationResult validate(String xml) {
        if (xml == null || xml.isBlank()) {
            return DrawioXmlValidationSupport.ValidationResult.valid();
        }

        Document document;
        try {
            document = parsingSupport.parse(xml);
        } catch (Exception e) {
            return DrawioXmlValidationSupport.ValidationResult.valid();
        }

        Element root = document.getDocumentElement();
        if (root == null) {
            return DrawioXmlValidationSupport.ValidationResult.valid();
        }

        NodeList diagrams = root.getElementsByTagName("diagram");
        if (diagrams.getLength() == 0) {
            return DrawioXmlValidationSupport.ValidationResult.valid();
        }
        Element graphModel = firstChildElement((Element) diagrams.item(0), "mxGraphModel");
        if (graphModel == null) {
            return DrawioXmlValidationSupport.ValidationResult.valid();
        }
        Element rootElement = firstChildElement(graphModel, "root");
        if (rootElement == null) {
            return DrawioXmlValidationSupport.ValidationResult.valid();
        }

        Map<String, Element> cellsById = collectCells(rootElement);
        DrawioXmlValidationSupport.ValidationResult containerResult = validateContainerLayout(cellsById);
        if (!containerResult.isValid()) {
            return containerResult;
        }

        DrawioXmlValidationSupport.ValidationResult edgeResult = validateEdgeLayout(cellsById);
        if (!edgeResult.isValid()) {
            return edgeResult;
        }

        return DrawioXmlValidationSupport.ValidationResult.valid();
    }

    private Map<String, Element> collectCells(Element rootElement) {
        Map<String, Element> cellsById = new LinkedHashMap<>();
        NodeList children = rootElement.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element cell && "mxCell".equals(cell.getTagName())) {
                cellsById.put(cell.getAttribute("id"), cell);
            }
        }
        return cellsById;
    }

    private DrawioXmlValidationSupport.ValidationResult validateContainerLayout(Map<String, Element> cellsById) {
        List<LayoutBox> containers = new ArrayList<>();
        for (Element cell : cellsById.values()) {
            if (!"1".equals(cell.getAttribute("vertex")) || !isContainer(cell)) {
                continue;
            }
            LayoutBox containerBox = toLayoutBox(cell);
            if (containerBox != null) {
                containers.add(containerBox);
            }
        }

        for (int i = 0; i < containers.size(); i++) {
            LayoutBox left = containers.get(i);
            for (int j = i + 1; j < containers.size(); j++) {
                LayoutBox right = containers.get(j);
                if (!sameParent(left.cell(), right.cell())) {
                    continue;
                }
                if (left.intersects(right)) {
                    return DrawioXmlValidationSupport.ValidationResult.invalid(
                            DrawioXmlValidationErrorCode.CONTAINER_OVERLAP,
                            "Container " + left.id() + " overlaps container " + right.id() + "."
                    );
                }
            }
        }

        for (LayoutBox container : containers) {
            for (Element cell : cellsById.values()) {
                if (!"1".equals(cell.getAttribute("vertex"))) {
                    continue;
                }
                if (!container.id().equals(cell.getAttribute("parent"))) {
                    continue;
                }
                LayoutBox child = toLayoutBox(cell);
                if (child == null) {
                    continue;
                }
                if (!container.contains(child, 16D)) {
                    return DrawioXmlValidationSupport.ValidationResult.invalid(
                            DrawioXmlValidationErrorCode.CONTAINER_CHILD_OUT_OF_BOUNDS,
                            "Container " + container.id() + " does not fully enclose child " + child.id() + " with padding."
                    );
                }
            }
        }

        return DrawioXmlValidationSupport.ValidationResult.valid();
    }

    private DrawioXmlValidationSupport.ValidationResult validateEdgeLayout(Map<String, Element> cellsById) {
        Map<EdgeKey, Integer> duplicatedPairCounts = new HashMap<>();
        Map<String, Integer> actorEdgeCounts = new HashMap<>();

        for (Element cell : cellsById.values()) {
            if (!"1".equals(cell.getAttribute("edge"))) {
                continue;
            }

            Element geometry = firstChildElement(cell, "mxGeometry");
            boolean hasCustomRouting = hasCustomRouting(geometry);
            String source = cell.getAttribute("source");
            String target = cell.getAttribute("target");

            if (!hasCustomRouting && !source.isBlank() && !target.isBlank()) {
                duplicatedPairCounts.merge(new EdgeKey(source, target), 1, Integer::sum);
            }

            if (isUseCaseStyleEdge(cell, cellsById) && !hasCustomRouting) {
                String actorId = actorEndpoint(source, target, cellsById);
                if (actorId != null) {
                    actorEdgeCounts.merge(actorId, 1, Integer::sum);
                }
            }
        }

        for (Map.Entry<EdgeKey, Integer> entry : duplicatedPairCounts.entrySet()) {
            if (entry.getValue() < 2) {
                continue;
            }
            DrawioXmlValidationErrorCode code = isLikelySequencePair(entry.getKey(), cellsById)
                    ? DrawioXmlValidationErrorCode.SEQUENCE_MESSAGE_BAND_CONFLICT
                    : DrawioXmlValidationErrorCode.EDGE_OVERLAP_RISK;
            String message = isLikelySequencePair(entry.getKey(), cellsById)
                    ? "Sequence messages between " + entry.getKey().source() + " and " + entry.getKey().target() + " share the same routing band."
                    : "Multiple edges between " + entry.getKey().source() + " and " + entry.getKey().target() + " share the same straight routing.";
            return DrawioXmlValidationSupport.ValidationResult.invalid(code, message);
        }

        for (Map.Entry<String, Integer> entry : actorEdgeCounts.entrySet()) {
            if (entry.getValue() > 1) {
                return DrawioXmlValidationSupport.ValidationResult.invalid(
                        DrawioXmlValidationErrorCode.USECASE_EDGE_OVERLAP,
                        "Actor " + entry.getKey() + " has multiple use case edges without distinct routing."
                );
            }
        }

        return DrawioXmlValidationSupport.ValidationResult.valid();
    }

    private boolean isContainer(Element cell) {
        String style = cell.getAttribute("style");
        return style != null && (style.contains("swimlane") || style.contains("container=1"));
    }

    private boolean sameParent(Element left, Element right) {
        return safe(left.getAttribute("parent")).equals(safe(right.getAttribute("parent")));
    }

    private LayoutBox toLayoutBox(Element cell) {
        Element geometry = firstChildElement(cell, "mxGeometry");
        if (geometry == null) {
            return null;
        }
        try {
            double x = Double.parseDouble(geometry.getAttribute("x"));
            double y = Double.parseDouble(geometry.getAttribute("y"));
            double width = Double.parseDouble(geometry.getAttribute("width"));
            double height = Double.parseDouble(geometry.getAttribute("height"));
            return new LayoutBox(cell.getAttribute("id"), cell, x, y, width, height);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean hasCustomRouting(Element geometry) {
        if (geometry == null) {
            return false;
        }
        if (firstChildElement(geometry, "Array") != null) {
            return true;
        }
        return firstChildElement(geometry, "mxPoint") != null;
    }

    private boolean isLikelySequencePair(EdgeKey edgeKey, Map<String, Element> cellsById) {
        Element source = cellsById.get(edgeKey.source());
        Element target = cellsById.get(edgeKey.target());
        return isSequenceParticipant(source) && isSequenceParticipant(target);
    }

    private boolean isSequenceParticipant(Element cell) {
        if (cell == null) {
            return false;
        }
        String style = safe(cell.getAttribute("style")).toLowerCase();
        String label = safe(cell.getAttribute("value")).toLowerCase();
        return style.contains("verticalalign=top")
                || style.contains("lifeline")
                || label.contains("participant")
                || label.contains("actor");
    }

    private boolean isUseCaseStyleEdge(Element edge, Map<String, Element> cellsById) {
        String actorId = actorEndpoint(edge.getAttribute("source"), edge.getAttribute("target"), cellsById);
        return actorId != null;
    }

    private String actorEndpoint(String source, String target, Map<String, Element> cellsById) {
        if (isActor(cellsById.get(source)) && isUseCase(cellsById.get(target))) {
            return source;
        }
        if (isActor(cellsById.get(target)) && isUseCase(cellsById.get(source))) {
            return target;
        }
        return null;
    }

    private boolean isActor(Element cell) {
        return cell != null && safe(cell.getAttribute("style")).contains("shape=umlActor");
    }

    private boolean isUseCase(Element cell) {
        if (cell == null) {
            return false;
        }
        String style = safe(cell.getAttribute("style")).toLowerCase();
        return style.contains("ellipse") || style.contains("usecase");
    }

    private Element firstChildElement(Element parent, String tagName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element element && tagName.equals(element.getTagName())) {
                return element;
            }
        }
        return null;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private record EdgeKey(String source, String target) {
    }

    private record LayoutBox(String id, Element cell, double x, double y, double width, double height) {
        private double right() {
            return x + width;
        }

        private double bottom() {
            return y + height;
        }

        private boolean intersects(LayoutBox other) {
            return x < other.right() && right() > other.x && y < other.bottom() && bottom() > other.y;
        }

        private boolean contains(LayoutBox other, double padding) {
            return other.x >= x + padding
                    && other.y >= y + padding
                    && other.right() <= right() - padding
                    && other.bottom() <= bottom() - padding;
        }
    }
}
