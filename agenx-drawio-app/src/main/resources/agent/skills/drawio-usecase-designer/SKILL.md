---
name: drawio-usecase-designer
description: Specialized guidance for use case diagrams with actors, system boundaries, and include or extend relations.
---

# Draw.io Use Case Designer

Use this skill when the user explicitly asks for a use case diagram or describes actors interacting with system capabilities rather than a time-ordered process.

## Applicable Inputs

- Product use case diagrams
- System actor-capability relationships
- Role-based access or responsibility overviews
- Functional requirement summaries that map to user goals

## Input Understanding Rules

- Identify actors first, then identify use cases as system capabilities.
- Determine the system boundary that contains the use cases.
- Distinguish actor-to-use-case associations from include and extend relations between use cases.
- If the input is PRD or requirements text, extract roles, goals, functional capabilities, and optional extensions before drawing.

## Output Constraints

- Actors must stay outside the system boundary.
- Use cases must stay inside the system boundary.
- Include and extend relations must remain use-case-to-use-case relations, not actor edges.
- In XML labels, render relation text as `<<include>>` and `<<extend>>`, not `<include>` or `<extend>`.
- Keep the capability set focused on user goals rather than internal implementation steps.
- In blueprint modes, make actor ownership and boundary membership explicit.

## Layout Rules

- Place the system boundary as the central container.
- Place actors around the left and right sides of the system boundary.
- Place core use cases near the center and related extensions nearby.
- Size the system boundary after placing all use cases so it fully encloses them with visible padding on all sides.
- Keep a clear safety margin between each use case and the system boundary border.
- Keep association edges short and readable.
- Fan actors vertically and spread use cases horizontally enough that actor associations do not collapse onto the same segment.
- Group use cases by domain only when the grouping improves clarity.

## Node And Edge Semantics

- Actors represent people, roles, systems, or external organizations.
- Use cases represent user-visible capabilities or goals.
- Association links connect actors to use cases.
- Include means required shared behavior.
- Extend means optional or conditional extension of a base use case.

## Reference Pattern

Use the following XML as a positive reference for a clean use case diagram. Follow its structural ideas and edge semantics, but adapt ids, labels, positions, and boundary size to the actual user request instead of copying it literally.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<mxfile host="app.diagrams.net">
  <diagram name="Page-1" id="bLx7p-QKm06H2kT_TVVF">
    <mxGraphModel dx="1221" dy="754" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="827" pageHeight="1169" math="0" shadow="0">
      <root>
        <mxCell id="0" />
        <mxCell id="1" parent="0" />
        <mxCell id="syGdotkE2nzxYbWcVMxL-1" parent="1" style="rounded=0;whiteSpace=wrap;html=1;strokeWidth=2;" value="" vertex="1">
          <mxGeometry height="420" width="620" x="140" y="70" as="geometry" />
        </mxCell>
        <mxCell id="syGdotkE2nzxYbWcVMxL-2" parent="1" style="shape=umlActor;verticalLabelPosition=bottom;verticalAlign=top;html=1;" value="用户" vertex="1">
          <mxGeometry height="120" width="60" x="40" y="240" as="geometry" />
        </mxCell>
        <mxCell id="syGdotkE2nzxYbWcVMxL-3" parent="1" style="ellipse;whiteSpace=wrap;html=1;" value="登录" vertex="1">
          <mxGeometry height="50" width="120" x="240" y="140" as="geometry" />
        </mxCell>
        <mxCell id="syGdotkE2nzxYbWcVMxL-4" parent="1" style="ellipse;whiteSpace=wrap;html=1;" value="浏览商品" vertex="1">
          <mxGeometry height="50" width="120" x="240" y="230" as="geometry" />
        </mxCell>
        <mxCell id="syGdotkE2nzxYbWcVMxL-5" parent="1" style="ellipse;whiteSpace=wrap;html=1;" value="下单" vertex="1">
          <mxGeometry height="50" width="120" x="240" y="320" as="geometry" />
        </mxCell>
        <mxCell id="syGdotkE2nzxYbWcVMxL-6" parent="1" style="ellipse;whiteSpace=wrap;html=1;" value="验证库存" vertex="1">
          <mxGeometry height="50" width="120" x="480" y="270" as="geometry" />
        </mxCell>
        <mxCell id="syGdotkE2nzxYbWcVMxL-7" parent="1" style="ellipse;whiteSpace=wrap;html=1;" value="计算优惠" vertex="1">
          <mxGeometry height="50" width="120" x="560" y="360" as="geometry" />
        </mxCell>
        <mxCell id="syGdotkE2nzxYbWcVMxL-8" parent="1" style="ellipse;whiteSpace=wrap;html=1;" value="支付订单" vertex="1">
          <mxGeometry height="50" width="120" x="480" y="140" as="geometry" />
        </mxCell>
        <mxCell id="syGdotkE2nzxYbWcVMxL-9" edge="1" parent="1" source="syGdotkE2nzxYbWcVMxL-2" style="endArrow=none;html=1;" target="syGdotkE2nzxYbWcVMxL-3" value="">
          <mxGeometry relative="1" as="geometry" />
        </mxCell>
        <mxCell id="syGdotkE2nzxYbWcVMxL-10" edge="1" parent="1" source="syGdotkE2nzxYbWcVMxL-2" style="endArrow=none;html=1;" target="syGdotkE2nzxYbWcVMxL-4" value="">
          <mxGeometry relative="1" as="geometry" />
        </mxCell>
        <mxCell id="syGdotkE2nzxYbWcVMxL-11" edge="1" parent="1" source="syGdotkE2nzxYbWcVMxL-2" style="endArrow=none;html=1;" target="syGdotkE2nzxYbWcVMxL-5" value="">
          <mxGeometry relative="1" as="geometry" />
        </mxCell>
        <mxCell id="syGdotkE2nzxYbWcVMxL-12" edge="1" parent="1" source="syGdotkE2nzxYbWcVMxL-5" style="dashed=1;endArrow=block;endFill=0;html=1;" target="syGdotkE2nzxYbWcVMxL-6" value="&amp;lt;&amp;lt;include&amp;gt;&amp;gt;">
          <mxGeometry relative="1" as="geometry" />
        </mxCell>
        <mxCell id="syGdotkE2nzxYbWcVMxL-13" edge="1" parent="1" source="syGdotkE2nzxYbWcVMxL-7" style="dashed=1;endArrow=block;endFill=0;html=1;" target="syGdotkE2nzxYbWcVMxL-5" value="&amp;lt;&amp;lt;extend&amp;gt;&amp;gt;">
          <mxGeometry relative="1" as="geometry" />
        </mxCell>
        <mxCell id="syGdotkE2nzxYbWcVMxL-14" edge="1" parent="1" source="syGdotkE2nzxYbWcVMxL-5" style="dashed=1;endArrow=block;endFill=0;html=1;" target="syGdotkE2nzxYbWcVMxL-8" value="&amp;lt;&amp;lt;include&amp;gt;&amp;gt;">
          <mxGeometry relative="1" as="geometry" />
        </mxCell>
      </root>
    </mxGraphModel>
  </diagram>
</mxfile>
```

## Avoid

- Do not model a use case diagram as a flowchart.
- Do not place actors inside the system boundary.
- Do not connect actors directly to include or extend edges.
- Do not use `<include>` or `<extend>` as XML label text when `html=1`; they can be parsed as tags and disappear.
- Do not turn low-level implementation steps into separate use cases unless the user explicitly wants that granularity.
- Do not let any use case touch, cross, or sit outside the system boundary.
- Do not shrink the system boundary so tightly that labels or ovals visually collide with the border.
- Do not let multiple actor associations or include/extend edges fully overlap; offset anchors or use waypoints when necessary.
