---
name: drawio-xml-designer
description: General-purpose draw.io XML guidance for requests that do not fit a more specialized diagram skill.
---

# Draw.io XML Designer

Use this skill when the user wants a diagram that should be rendered as draw.io XML.

## Objective

Generate XML that draw.io can import directly while preserving a clean structure, readable layout, and stable geometry.

## Output Contract

- Output XML only.
- The root element must be `<mxfile>`.
- Each page must be inside a `<diagram>` element.
- Each diagram must contain an `<mxGraphModel>`.
- Include:
  - `<mxCell id="0"/>`
  - `<mxCell id="1" parent="0"/>`
- Every visible node uses `vertex="1"`.
- Every connection uses `edge="1"`.
- Every node and edge must include `<mxGeometry ... as="geometry"/>`.

## Recommended Skeleton

```xml
<mxfile host="app.diagrams.net" modified="2026-04-02T00:00:00.000Z" agent="drawioXmlAgent" version="26.0.0">
  <diagram id="page-1" name="Page-1">
    <mxGraphModel dx="1600" dy="1200" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="1600" pageHeight="1200" math="0" shadow="0">
      <root>
        <mxCell id="0"/>
        <mxCell id="1" parent="0"/>
      </root>
    </mxGraphModel>
  </diagram>
</mxfile>
```

## Layout Rules

- Flowcharts: top-to-bottom unless the request strongly suggests left-to-right.
- Architecture diagrams: left-to-right or layered, with containers for bounded areas.
- Sequence-like diagrams: actors left-to-right, interactions top-to-bottom.
- Mind maps: center the root topic and branch outward consistently.
- Avoid overlaps and leave whitespace between groups.
- Keep a single dominant reading direction and avoid unnecessary backtracking edges.
- Prefer aligned rows or columns for sibling nodes.
- Prefer orthogonal connectors and route them around groups to reduce crossings.
- For complex diagrams, split the graph into clear regions or containers before adding edges.
- For diagrams with containers, place inner nodes or child containers first, then size each parent container to fully enclose its contents.
- Parent containers must fully enclose internal nodes, child containers, and connector routing space with visible padding on every side.
- Do not let container borders overlap child containers, nodes, labels, or connector lines.
- If a container becomes tight, expand the container or canvas instead of compressing the contents.

## Node Patterns

- Start/end:
  - `ellipse;whiteSpace=wrap;html=1;aspect=fixed;fillColor=#D5E8D4;strokeColor=#82B366;fontColor=#1A1A1A;`
- Process:
  - `rounded=1;whiteSpace=wrap;html=1;fillColor=#DAE8FC;strokeColor=#6C8EBF;fontColor=#1A1A1A;`
- Decision:
  - `rhombus;whiteSpace=wrap;html=1;fillColor=#FFF2CC;strokeColor=#D6B656;fontColor=#1A1A1A;`
- Data store:
  - `shape=cylinder;whiteSpace=wrap;html=1;boundedLbl=1;fillColor=#E1D5E7;strokeColor=#9673A6;fontColor=#1A1A1A;`
- Note:
  - `shape=note;whiteSpace=wrap;html=1;fillColor=#F5F5F5;strokeColor=#666666;fontColor=#1A1A1A;`
- Container/swimlane:
  - `swimlane;whiteSpace=wrap;html=1;fillColor=#F8F9FA;strokeColor=#B0B7C3;fontColor=#1A1A1A;`

## Edge Patterns

- Default edge style:
  - `edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;endArrow=block;endFill=1;`
- Use edge labels only when they add information such as conditions, protocols, or message names.
- Keep edge styles consistent within one diagram; do not mix decorative arrows or random line colors without semantic meaning.

## Quality Bar

- Keep ids unique.
- Keep labels concise.
- Escape XML-sensitive characters.
- Prefer stable, readable geometry over dense packing.
- Use one consistent style for each logical node type across the entire diagram.
- If a diagram becomes dense, prefer more whitespace and clearer grouping over compactness.
- If the user asks for a fun or unusual diagram, preserve correctness first and add variation through layout, grouping, and labeling rather than risky XML tricks.
