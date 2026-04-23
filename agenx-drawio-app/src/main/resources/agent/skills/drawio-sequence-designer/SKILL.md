---
name: drawio-sequence-designer
description: Specialized guidance for sequence diagrams, interaction timelines, and ordered message flows.
---

# Draw.io Sequence Designer

Use this skill when the user explicitly asks for a sequence diagram, interaction diagram, call flow, request-response chain, or any request centered on time-ordered messages between participants.

## Applicable Inputs

- User to system interaction sequences
- Service-to-service call chains
- API request and response flows
- Event, callback, and async interaction timelines

## Input Understanding Rules

- Identify participants first, then order them left to right.
- Identify messages in time order from top to bottom.
- Distinguish synchronous calls, asynchronous messages, returns, callbacks, and internal processing.
- If the request comes from PRD or architecture prose, extract actors, services, triggers, responses, and side effects before drawing.

## Output Constraints

- Preserve participant order and message order explicitly.
- Keep each message tied to a clear sender and receiver.
- Use one vertical time direction from top to bottom.
- In blueprint modes, make participant order and message order explicit in the structure.
- In XML modes, keep the sequence semantics visible and do not collapse the diagram into a generic flowchart.
- In XML modes, every message row must have a unique y band; do not reuse the same vertical band for different stages.
- In XML modes, prefer the standard draw.io lifeline structure instead of improvising a generic box-and-arrow layout.

## Layout Rules

- Place participants left to right across the top.
- Place each participant's timeline vertically below it.
- Put messages on separate horizontal bands ordered by time.
- Keep participant lanes clearly separated, typically with at least 220px horizontal spacing.
- Keep message rows clearly separated, typically with at least 80px vertical spacing, and use more when labels are long.
- Assign every message or step to its own horizontal band with strictly increasing y coordinates.
- Do not place multiple steps in the same horizontal band.
- Do not reuse a vertical region that already belongs to an earlier step for a later step.
- If two messages involve the same participant pair, do not place them on the same horizontal path; use separate y bands and, when needed, distinct elbow offsets.
- Keep participant headers at one top row and do not place later interaction blocks back into the same top area.
- Increase page height for long conversations instead of compressing rows until they overlap.
- Keep return messages visually distinct only when they carry useful meaning.
- Group optional or related interactions with notes or containers only when they improve readability.
- A stable baseline is: participant width about 100, participant x gap about 180 to 240, first message row near y=220, row gap about 70 to 90.
- Lifeline headers stay on the same top row. Activation bars stay inside their participant container and align to the message rows they belong to.
- Prefer expanding the lifeline height and page height over shrinking row gaps.

## Node And Edge Semantics

- Participants are actors, users, systems, services, databases, or external integrations.
- Messages represent ordered interactions, not generic dependencies.
- Returns should point back to the caller.
- Self calls are allowed only for internal processing on the same participant.
- Activation or focus should follow message handling rather than arbitrary decoration.
- Use `shape=umlLifeline` for participants when generating draw.io XML.
- Use slim activation bars inside the participant container for active processing windows.
- Use solid arrows for forward calls and dashed arrows for returns when the return itself is meaningful.
- If repeated calls or callbacks between the same two participants would visually merge, separate them with staggered rows and explicit waypoint offsets instead of reusing the same segment.

## XML Skeleton Pattern

When generating draw.io XML for sequence diagrams, prefer this structure and adapt only the labels, coordinates, counts, and activation ranges:

```xml
<mxfile host="embed.diagrams.net">
    <diagram name="用户支付时序图" id="0">
        <mxGraphModel dx="377" dy="506" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="1500" pageHeight="800" math="0" shadow="0">
            <root>
                <mxCell id="0" />
                <mxCell id="1" parent="0" />
                <mxCell id="315" parent="1" style="shape=umlLifeline;perimeter=lifelinePerimeter;whiteSpace=wrap;html=1;container=1;collapsible=0;recursiveResize=0;outlineConnect=0;" value="&lt;b&gt;前端&lt;/b&gt;" vertex="1">
                    <mxGeometry height="400" width="100" x="720" y="150" as="geometry" />
                </mxCell>
                <mxCell id="328" parent="315" style="html=1;points=[];perimeter=orthogonalPerimeter;rounded=0;shadow=0;strokeWidth=1;fillColor=#dae8fc;" value="" vertex="1">
                    <mxGeometry height="200" width="10" x="45" y="70" as="geometry" />
                </mxCell>
                <mxCell id="316" parent="1" style="shape=umlLifeline;perimeter=lifelinePerimeter;whiteSpace=wrap;html=1;container=1;collapsible=0;recursiveResize=0;outlineConnect=0;" value="&lt;b&gt;后端&lt;/b&gt;" vertex="1">
                    <mxGeometry height="400" width="100" x="920" y="150" as="geometry" />
                </mxCell>
                <mxCell id="317" parent="316" style="html=1;points=[];perimeter=orthogonalPerimeter;rounded=0;shadow=0;strokeWidth=1;fillColor=#dae8fc;" value="" vertex="1">
                    <mxGeometry height="120" width="10" x="45" y="120" as="geometry" />
                </mxCell>
                <mxCell id="324" edge="1" parent="316" style="html=1;verticalAlign=bottom;endArrow=block;rounded=0;dashed=1;" target="315" value="JSON结果">
                    <mxGeometry relative="1" as="geometry">
                        <mxPoint x="45" y="240" as="sourcePoint" />
                        <mxPoint x="-154.57142857142867" y="240" as="targetPoint" />
                    </mxGeometry>
                </mxCell>
                <mxCell id="318" parent="1" style="shape=umlLifeline;perimeter=lifelinePerimeter;whiteSpace=wrap;html=1;container=1;collapsible=0;recursiveResize=0;outlineConnect=0;" value="&lt;b&gt;数据库&lt;/b&gt;" vertex="1">
                    <mxGeometry height="400" width="100" x="1120" y="150" as="geometry" />
                </mxCell>
                <mxCell id="319" parent="318" style="html=1;points=[];perimeter=orthogonalPerimeter;rounded=0;shadow=0;strokeWidth=1;fillColor=#dae8fc;" value="" vertex="1">
                    <mxGeometry height="40" width="10" x="45" y="160" as="geometry" />
                </mxCell>
                <mxCell id="322" edge="1" parent="1" source="316" style="html=1;verticalAlign=bottom;endArrow=block;entryX=0;entryY=0;rounded=0;" value="SELECT">
                    <mxGeometry relative="1" as="geometry">
                        <mxPoint x="969.9285714285713" y="310" as="sourcePoint" />
                        <mxPoint x="1169.5" y="310" as="targetPoint" />
                    </mxGeometry>
                </mxCell>
                <mxCell id="323" edge="1" parent="1" source="318" style="html=1;verticalAlign=bottom;endArrow=block;rounded=0;dashed=1;" target="316" value="返回数据">
                    <mxGeometry relative="1" as="geometry">
                        <mxPoint x="1169.5" y="349.71000000000004" as="sourcePoint" />
                        <mxPoint x="969.5" y="349.71000000000004" as="targetPoint" />
                    </mxGeometry>
                </mxCell>
                <mxCell id="325" edge="1" parent="1" style="html=1;verticalAlign=bottom;endArrow=block;rounded=0;" target="326" value="渲染页面">
                    <mxGeometry relative="1" as="geometry">
                        <mxPoint x="769.5" y="420.0000000000001" as="sourcePoint" />
                        <mxPoint x="559.9999999999999" y="420.0000000000001" as="targetPoint" />
                    </mxGeometry>
                </mxCell>
                <mxCell id="326" parent="1" style="shape=umlLifeline;perimeter=lifelinePerimeter;whiteSpace=wrap;html=1;container=1;collapsible=0;recursiveResize=0;outlineConnect=0;" value="&lt;b&gt;用户&lt;/b&gt;" vertex="1">
                    <mxGeometry height="400" width="100" x="500" y="150" as="geometry" />
                </mxCell>
                <mxCell id="320" edge="1" parent="1" style="html=1;verticalAlign=bottom;endArrow=block;rounded=0;" value="点击按钮">
                    <mxGeometry relative="1" as="geometry">
                        <mxPoint x="549.810344827586" y="219.9999999999999" as="sourcePoint" />
                        <mxPoint x="769.5" y="219.9999999999999" as="targetPoint" />
                    </mxGeometry>
                </mxCell>
                <mxCell id="321" edge="1" parent="1" style="html=1;verticalAlign=bottom;endArrow=block;rounded=0;" value="POST /api/data">
                    <mxGeometry relative="1" as="geometry">
                        <mxPoint x="769.9285714285713" y="270" as="sourcePoint" />
                        <mxPoint x="969.5" y="270" as="targetPoint" />
                    </mxGeometry>
                </mxCell>
            </root>
        </mxGraphModel>
    </diagram>
</mxfile>
```

Use the pattern above as a template, not as fixed content:

- Keep all participant lifelines as top-level children of `parent="1"`.
- Keep activation bars as children of their participant lifeline, not as free-floating top-level nodes.
- Keep each message edge on `parent="1"` so arrows can span across lifelines cleanly.
- For message row `n`, increase the y coordinate from the previous row; do not keep two different messages at the same y.
- When there are many steps, extend participant height first, then extend page height.
- When there are many participants, add more horizontal spacing instead of squeezing lifelines together.

## Avoid

- Do not place sequence steps on a single process axis like a flowchart.
- Do not mix message order with structural dependency order.
- Do not omit sender or receiver identity.
- Do not turn include, extend, or business rules into separate participants.
- Do not stack multiple stages into the same vertical region so that earlier and later interactions overlap.
- Do not place participant headers, notes, and message blocks on top of each other to save space.
- Do not collapse sequence interactions into overlapping process blocks or generic flowchart rows.
- Do not allow two message arrows, dashed returns, or self-calls to visually coincide on the same line segment.

## Anti-pattern Example

- Bad pattern:
  Two different messages between the same participant pair are placed on the same y band, so the solid call, dashed return, or callback visually merge into one line.
- Why it is bad:
  Readers cannot tell whether there was one interaction or several ordered interactions, and labels or arrowheads become ambiguous.
- Correct fix:
  Give each message its own strictly increasing y band. If the same participant pair communicates multiple times, stagger the rows and, when needed, add distinct elbow offsets or waypoints so no two interactions share the same long segment.
