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

## Node Patterns

- Start/end:
  - `ellipse;whiteSpace=wrap;html=1;aspect=fixed;`
- Process:
  - `rounded=1;whiteSpace=wrap;html=1;`
- Decision:
  - `rhombus;whiteSpace=wrap;html=1;`
- Data store:
  - `shape=cylinder;whiteSpace=wrap;html=1;boundedLbl=1;`
- Note:
  - `shape=note;whiteSpace=wrap;html=1;`
- Container/swimlane:
  - `swimlane;whiteSpace=wrap;html=1;`

## Edge Patterns

- Default edge style:
  - `edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;endArrow=block;endFill=1;`
- Use edge labels only when they add information such as conditions, protocols, or message names.

## Quality Bar

- Keep ids unique.
- Keep labels concise.
- Escape XML-sensitive characters.
- Prefer stable, readable geometry over dense packing.
- If the user asks for a fun or unusual diagram, preserve correctness first and add variation through layout, grouping, and labeling rather than risky XML tricks.
