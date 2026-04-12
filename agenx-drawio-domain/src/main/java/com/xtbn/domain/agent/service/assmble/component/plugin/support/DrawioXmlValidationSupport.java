package com.xtbn.domain.agent.service.assmble.component.plugin.support;

import lombok.Builder;
import lombok.Getter;
import com.xtbn.types.enums.DrawioXmlValidationErrorCode;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class DrawioXmlValidationSupport {

    public ValidationResult validate(String xml) {
        if (xml == null || xml.isBlank()) {
            return ValidationResult.invalid("EMPTY_XML", "XML content is empty.");
        }

        Document document;
        try {
            DocumentBuilderFactory factory = secureFactory();
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(new InputSource(new StringReader(xml)));
        } catch (Exception e) {
            return ValidationResult.invalid("XML_SYNTAX_ERROR", sanitizeMessage(e.getMessage()));
        }

        Element root = document.getDocumentElement();
        if (root == null || !"mxfile".equals(root.getTagName())) {
            return ValidationResult.invalid("ROOT_NOT_MXFILE", "Root element must be mxfile.");
        }

        NodeList diagrams = root.getElementsByTagName("diagram");
        if (diagrams.getLength() == 0) {
            return ValidationResult.invalid("MISSING_DIAGRAM", "mxfile must contain at least one diagram.");
        }

        Element graphModel = firstChildElement((Element) diagrams.item(0), "mxGraphModel");
        if (graphModel == null) {
            return ValidationResult.invalid("MISSING_GRAPH_MODEL", "diagram must contain mxGraphModel.");
        }

        Element rootElement = firstChildElement(graphModel, "root");
        if (rootElement == null) {
            return ValidationResult.invalid("MISSING_ROOT", "mxGraphModel must contain root.");
        }

        return validateCells(rootElement);
    }

    private ValidationResult validateCells(Element rootElement) {
        Map<String, Element> cellsById = new HashMap<>();
        List<Element> cells = new ArrayList<>();
        Set<String> duplicateIds = new HashSet<>();

        NodeList children = rootElement.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (!(node instanceof Element cell) || !"mxCell".equals(cell.getTagName())) {
                continue;
            }
            cells.add(cell);
            String id = cell.getAttribute("id");
            if (id == null || id.isBlank()) {
                return ValidationResult.invalid("MISSING_CELL_ID", "Every mxCell must have a non-empty id.");
            }
            if (cellsById.putIfAbsent(id, cell) != null) {
                duplicateIds.add(id);
            }
        }

        if (!duplicateIds.isEmpty()) {
            return ValidationResult.invalid("DUPLICATE_CELL_ID", "Duplicate mxCell ids: " + String.join(", ", duplicateIds));
        }

        Element cell0 = cellsById.get("0");
        Element cell1 = cellsById.get("1");
        if (cell0 == null) {
            return ValidationResult.invalid("MISSING_ROOT_CELL_0", "mxCell id=0 is required.");
        }
        if (cell1 == null || !"0".equals(cell1.getAttribute("parent"))) {
            return ValidationResult.invalid("MISSING_ROOT_CELL_1", "mxCell id=1 with parent=0 is required.");
        }

        for (Element cell : cells) {
            String id = cell.getAttribute("id");
            String parent = cell.getAttribute("parent");
            if (!parent.isBlank() && !cellsById.containsKey(parent)) {
                return ValidationResult.invalid("INVALID_PARENT", "mxCell " + id + " references missing parent " + parent + ".");
            }

            boolean vertex = "1".equals(cell.getAttribute("vertex"));
            boolean edge = "1".equals(cell.getAttribute("edge"));
            if (vertex && edge) {
                return ValidationResult.invalid("CELL_ROLE_CONFLICT", "mxCell " + id + " cannot be both vertex and edge.");
            }

            Element geometry = firstChildElement(cell, "mxGeometry");
            if (vertex) {
                if (geometry == null || !"geometry".equals(geometry.getAttribute("as"))) {
                    return ValidationResult.invalid("VERTEX_GEOMETRY_MISSING", "Vertex " + id + " must contain mxGeometry as=geometry.");
                }
                if (!isNumeric(geometry.getAttribute("x")) || !isNumeric(geometry.getAttribute("y"))) {
                    return ValidationResult.invalid("VERTEX_COORDINATE_INVALID", "Vertex " + id + " must have numeric x and y.");
                }
                if (!isPositiveNumber(geometry.getAttribute("width")) || !isPositiveNumber(geometry.getAttribute("height"))) {
                    return ValidationResult.invalid("VERTEX_SIZE_INVALID", "Vertex " + id + " must have positive width and height.");
                }
            }

            if (edge) {
                if (geometry == null || !"geometry".equals(geometry.getAttribute("as")) || !"1".equals(geometry.getAttribute("relative"))) {
                    return ValidationResult.invalid("EDGE_GEOMETRY_MISSING", "Edge " + id + " must contain mxGeometry relative=1 as=geometry.");
                }
                String source = cell.getAttribute("source");
                String target = cell.getAttribute("target");
                if (source.isBlank() || target.isBlank()) {
                    return ValidationResult.invalid("EDGE_ENDPOINT_MISSING", "Edge " + id + " must contain source and target.");
                }
                if (!cellsById.containsKey(source) || !cellsById.containsKey(target)) {
                    return ValidationResult.invalid("EDGE_ENDPOINT_INVALID", "Edge " + id + " references missing source or target.");
                }
            }
        }

        return ValidationResult.valid();
    }

    private DocumentBuilderFactory secureFactory() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setExpandEntityReferences(false);
        factory.setXIncludeAware(false);
        return factory;
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

    private boolean isNumeric(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isPositiveNumber(String value) {
        if (!isNumeric(value)) {
            return false;
        }
        return Double.parseDouble(value) > 0D;
    }

    private String sanitizeMessage(String message) {
        if (message == null || message.isBlank()) {
            return "XML parsing failed.";
        }
        return message.replaceAll("\\s+", " ").trim();
    }

    @Getter
    @Builder
    public static class ValidationResult {
        private final boolean valid;
        private final DrawioXmlValidationErrorCode errorCode;
        private final String errorMessage;

        public static ValidationResult valid() {
            return ValidationResult.builder().valid(true).build();
        }

        public static ValidationResult invalid(String errorCode, String errorMessage) {
            return invalid(DrawioXmlValidationErrorCode.valueOf(errorCode), errorMessage);
        }

        public static ValidationResult invalid(DrawioXmlValidationErrorCode errorCode, String errorMessage) {
            return ValidationResult.builder()
                    .valid(false)
                    .errorCode(errorCode)
                    .errorMessage(errorMessage)
                    .build();
        }
    }
}
