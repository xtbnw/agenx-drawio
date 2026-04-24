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
- In XML modes, repeated messages must use distinct routing points or waypoints, not only different y values.

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
- If two messages involve the same participant pair, give them distinct source or target points or waypoints so draw.io does not auto-merge them.
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
<?xml version="1.0" encoding="UTF-8"?>
<mxfile host="app.diagrams.net" agent="5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) draw.io/16.5.1 Chrome/96.0.4664.110 Electron/16.0.7 Safari/537.36" pages="2">
  <diagram id="i7KtP-Vs8fw_sYRqWalm" name="Sequence diagram">
    <mxGraphModel dx="1436" dy="887" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="1019" pageHeight="1320" math="0" shadow="0">
      <root>
        <mxCell id="0" />
        <mxCell id="1" parent="0" />
        <mxCell id="XppZFFv2hi1EjOijFOD9-1" parent="1" style="shape=umlFrame;whiteSpace=wrap;html=1;fillColor=#f5f5f5;fontColor=#333333;strokeColor=#666666;" value="alt" vertex="1">
          <mxGeometry height="330" width="620" x="200" y="240" as="geometry" />
        </mxCell>
        <mxCell id="XppZFFv2hi1EjOijFOD9-2" parent="1" style="shape=umlLifeline;participant=umlActor;perimeter=lifelinePerimeter;html=1;container=1;collapsible=0;recursiveResize=0;verticalAlign=top;spacingTop=36;outlineConnect=0;size=40;fillColor=#f8cecc;strokeColor=#b85450;" value=":Customer" vertex="1">
          <mxGeometry height="530" width="20" x="130" y="80" as="geometry" />
        </mxCell>
        <mxCell id="XppZFFv2hi1EjOijFOD9-3" parent="XppZFFv2hi1EjOijFOD9-2" style="html=1;points=[];perimeter=orthogonalPerimeter;fillColor=#f8cecc;strokeColor=#b85450;" value="" vertex="1">
          <mxGeometry height="420" width="10" x="5" y="70" as="geometry" />
        </mxCell>
        <mxCell id="XppZFFv2hi1EjOijFOD9-4" parent="1" style="shape=umlLifeline;perimeter=lifelinePerimeter;whiteSpace=wrap;html=1;container=1;collapsible=0;recursiveResize=0;outlineConnect=0;fillColor=#ffe6cc;strokeColor=#d79b00;" value=":SearchForm" vertex="1">
          <mxGeometry height="520" width="100" x="270" y="90" as="geometry" />
        </mxCell>
        <mxCell id="XppZFFv2hi1EjOijFOD9-5" parent="XppZFFv2hi1EjOijFOD9-4" style="html=1;points=[];perimeter=orthogonalPerimeter;fillColor=#ffe6cc;strokeColor=#d79b00;" value="" vertex="1">
          <mxGeometry height="380" width="10" x="45" y="80" as="geometry" />
        </mxCell>
        <mxCell id="XppZFFv2hi1EjOijFOD9-6" parent="XppZFFv2hi1EjOijFOD9-4" style="html=1;points=[];perimeter=orthogonalPerimeter;fillColor=#ffe6cc;strokeColor=#d79b00;" value="" vertex="1">
          <mxGeometry height="40" width="10" x="50" y="110" as="geometry" />
        </mxCell>
        <mxCell id="XppZFFv2hi1EjOijFOD9-7" edge="1" parent="XppZFFv2hi1EjOijFOD9-4" style="edgeStyle=orthogonalEdgeStyle;html=1;align=left;spacingLeft=2;endArrow=block;rounded=0;entryX=1;entryY=0;" target="XppZFFv2hi1EjOijFOD9-6" value="1.1: validSearch()">
          <mxGeometry relative="1" as="geometry">
            <Array as="points">
              <mxPoint x="80" y="100" />
              <mxPoint x="80" y="110" />
            </Array>
            <mxPoint x="55" y="100" as="sourcePoint" />
          </mxGeometry>
        </mxCell>
        <mxCell id="XppZFFv2hi1EjOijFOD9-8" parent="XppZFFv2hi1EjOijFOD9-4" style="html=1;points=[];perimeter=orthogonalPerimeter;fillColor=#ffe6cc;strokeColor=#d79b00;" value="" vertex="1">
          <mxGeometry height="40" width="10" x="50" y="380" as="geometry" />
        </mxCell>
        <mxCell id="XppZFFv2hi1EjOijFOD9-9" edge="1" parent="XppZFFv2hi1EjOijFOD9-4" source="XppZFFv2hi1EjOijFOD9-5" style="edgeStyle=orthogonalEdgeStyle;html=1;align=left;spacingLeft=2;endArrow=block;rounded=0;entryX=1;entryY=0;" target="XppZFFv2hi1EjOijFOD9-8" value="1.3: displayError()">
          <mxGeometry relative="1" as="geometry">
            <Array as="points">
              <mxPoint x="80" y="370" />
              <mxPoint x="80" y="380" />
            </Array>
            <mxPoint x="50" y="320" as="sourcePoint" />
          </mxGeometry>
        </mxCell>
        <mxCell id="XppZFFv2hi1EjOijFOD9-10" edge="1" parent="1" source="XppZFFv2hi1EjOijFOD9-3" style="html=1;verticalAlign=bottom;endArrow=block;entryX=0;entryY=0;rounded=0;" target="XppZFFv2hi1EjOijFOD9-5" value="1: itemSearch(itemName)">
          <mxGeometry relative="1" as="geometry">
            <mxPoint x="245" y="170" as="sourcePoint" />
          </mxGeometry>
        </mxCell>
        <mxCell id="XppZFFv2hi1EjOijFOD9-11" parent="1" style="shape=umlLifeline;perimeter=lifelinePerimeter;whiteSpace=wrap;html=1;container=1;collapsible=0;recursiveResize=0;outlineConnect=0;fillColor=#dae8fc;strokeColor=#6c8ebf;" value=":SearchResults" vertex="1">
          <mxGeometry height="520" width="100" x="490" y="90" as="geometry" />
        </mxCell>
        <mxCell id="XppZFFv2hi1EjOijFOD9-12" parent="XppZFFv2hi1EjOijFOD9-11" style="html=1;points=[];perimeter=orthogonalPerimeter;fillColor=#dae8fc;strokeColor=#6c8ebf;" value="" vertex="1">
          <mxGeometry height="20" width="10" x="45" y="250" as="geometry" />
        </mxCell>
        <mxCell id="XppZFFv2hi1EjOijFOD9-13" parent="1" style="shape=umlLifeline;participant=umlEntity;perimeter=lifelinePerimeter;whiteSpace=wrap;html=1;container=1;collapsible=0;recursiveResize=0;verticalAlign=top;spacingTop=36;outlineConnect=0;fillColor=#e1d5e7;strokeColor=#9673a6;" value=":ItemDatabase" vertex="1">
          <mxGeometry height="520" width="40" x="660" y="90" as="geometry" />
        </mxCell>
        <mxCell id="XppZFFv2hi1EjOijFOD9-14" parent="XppZFFv2hi1EjOijFOD9-13" style="html=1;points=[];perimeter=orthogonalPerimeter;fillColor=#e1d5e7;strokeColor=#9673a6;" value="" vertex="1">
          <mxGeometry height="40" width="10" x="15" y="180" as="geometry" />
        </mxCell>
        <mxCell id="XppZFFv2hi1EjOijFOD9-15" parent="1" style="shape=umlLifeline;perimeter=lifelinePerimeter;whiteSpace=wrap;html=1;container=1;collapsible=0;recursiveResize=0;outlineConnect=0;fillColor=#d5e8d4;strokeColor=#82b366;" value=":ResultList" vertex="1">
          <mxGeometry height="220" width="100" x="740" y="160" as="geometry" />
        </mxCell>
        <mxCell id="XppZFFv2hi1EjOijFOD9-16" parent="XppZFFv2hi1EjOijFOD9-15" style="shape=umlDestroy;whiteSpace=wrap;html=1;strokeWidth=3;" value="" vertex="1">
          <mxGeometry height="30" width="30" x="35" y="200" as="geometry" />
        </mxCell>
        <mxCell id="XppZFFv2hi1EjOijFOD9-17" parent="XppZFFv2hi1EjOijFOD9-15" style="html=1;points=[];perimeter=orthogonalPerimeter;fillColor=#d5e8d4;strokeColor=#82b366;" value="" vertex="1">
          <mxGeometry height="65" width="10" x="45" y="115" as="geometry" />
        </mxCell>
        <mxCell id="XppZFFv2hi1EjOijFOD9-18" edge="1" parent="1" source="XppZFFv2hi1EjOijFOD9-5" style="html=1;verticalAlign=bottom;endArrow=block;entryX=0;entryY=0;rounded=0;" target="XppZFFv2hi1EjOijFOD9-14" value="1.2: SearchItems(itemName)">
          <mxGeometry relative="1" as="geometry">
            <mxPoint x="610" y="200" as="sourcePoint" />
          </mxGeometry>
        </mxCell>
        <mxCell id="XppZFFv2hi1EjOijFOD9-19" edge="1" parent="1" source="XppZFFv2hi1EjOijFOD9-14" style="html=1;verticalAlign=bottom;endArrow=block;entryX=0;entryY=0;rounded=0;" target="XppZFFv2hi1EjOijFOD9-17" value="1.2.1: listResults()">
          <mxGeometry relative="1" as="geometry">
            <mxPoint x="722" y="285" as="sourcePoint" />
          </mxGeometry>
        </mxCell>
        <mxCell id="XppZFFv2hi1EjOijFOD9-20" edge="1" parent="1" source="XppZFFv2hi1EjOijFOD9-17" style="html=1;verticalAlign=bottom;endArrow=block;entryX=1;entryY=0;rounded=0;" target="XppZFFv2hi1EjOijFOD9-12" value="1.2.1.1: displayResults()">
          <mxGeometry relative="1" as="geometry">
            <Array as="points">
              <mxPoint x="610" y="340" />
            </Array>
            <mxPoint x="610" y="320" as="sourcePoint" />
          </mxGeometry>
        </mxCell>
        <mxCell id="XppZFFv2hi1EjOijFOD9-21" edge="1" parent="1" source="XppZFFv2hi1EjOijFOD9-1" style="endArrow=none;dashed=1;html=1;rounded=0;entryX=1;entryY=0.576;entryDx=0;entryDy=0;entryPerimeter=0;exitX=0;exitY=0.573;exitDx=0;exitDy=0;exitPerimeter=0;" target="XppZFFv2hi1EjOijFOD9-1" value="">
          <mxGeometry height="50" relative="1" width="50" as="geometry">
            <mxPoint x="410" y="380" as="sourcePoint" />
            <mxPoint x="460" y="330" as="targetPoint" />
          </mxGeometry>
        </mxCell>
        <mxCell id="XppZFFv2hi1EjOijFOD9-22" parent="1" style="text;html=1;align=center;verticalAlign=middle;resizable=0;points=[];autosize=1;strokeColor=none;fillColor=none;" value="[itemName=valid]" vertex="1">
          <mxGeometry height="20" width="110" x="200" y="270" as="geometry" />
        </mxCell>
        <mxCell id="XppZFFv2hi1EjOijFOD9-23" parent="1" style="text;html=1;align=center;verticalAlign=middle;resizable=0;points=[];autosize=1;strokeColor=none;fillColor=none;" value="[else]" vertex="1">
          <mxGeometry height="20" width="40" x="200" y="430" as="geometry" />
        </mxCell>
        <mxCell id="XppZFFv2hi1EjOijFOD9-24" edge="1" parent="1" source="XppZFFv2hi1EjOijFOD9-5" style="edgeStyle=none;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;endArrow=open;endFill=0;dashed=1;" target="XppZFFv2hi1EjOijFOD9-3">
          <mxGeometry relative="1" as="geometry">
            <Array as="points">
              <mxPoint x="230" y="540" />
            </Array>
          </mxGeometry>
        </mxCell>
      </root>
    </mxGraphModel>
  </diagram>
  <diagram id="hbYBUEr95w08JzggDiKY" name="With loop">
    <mxGraphModel dx="1332" dy="983" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="1019" pageHeight="1320" math="0" shadow="0">
      <root>
        <mxCell id="jC507fco_XKDHWjVbaC3-0" />
        <mxCell id="jC507fco_XKDHWjVbaC3-1" parent="jC507fco_XKDHWjVbaC3-0" />
        <mxCell id="jC507fco_XKDHWjVbaC3-2" value="sd example with a loop" style="shape=umlFrame;whiteSpace=wrap;html=1;width=150;height=30;boundedLbl=1;verticalAlign=middle;align=left;spacingLeft=5;fillColor=#f5f5f5;fontColor=#333333;strokeColor=#666666;" vertex="1" parent="jC507fco_XKDHWjVbaC3-1">
          <mxGeometry x="20" y="30" width="740" height="490" as="geometry" />
        </mxCell>
        <mxCell id="jC507fco_XKDHWjVbaC3-3" value=":Object" style="shape=umlLifeline;perimeter=lifelinePerimeter;whiteSpace=wrap;html=1;container=1;collapsible=0;recursiveResize=0;outlineConnect=0;fillColor=#dae8fc;strokeColor=#6c8ebf;" vertex="1" parent="jC507fco_XKDHWjVbaC3-1">
          <mxGeometry x="320" y="80" width="100" height="400" as="geometry" />
        </mxCell>
        <mxCell id="jC507fco_XKDHWjVbaC3-4" value="" style="html=1;points=[];perimeter=orthogonalPerimeter;" vertex="1" parent="jC507fco_XKDHWjVbaC3-3">
          <mxGeometry x="45" y="80" width="10" height="240" as="geometry" />
        </mxCell>
        <mxCell id="jC507fco_XKDHWjVbaC3-5" value="" style="html=1;points=[];perimeter=orthogonalPerimeter;" vertex="1" parent="jC507fco_XKDHWjVbaC3-3">
          <mxGeometry x="50" y="120" width="10" height="55" as="geometry" />
        </mxCell>
        <mxCell id="jC507fco_XKDHWjVbaC3-6" value="self call" style="edgeStyle=orthogonalEdgeStyle;html=1;align=left;spacingLeft=2;endArrow=block;rounded=0;entryX=1;entryY=0;" edge="1" parent="jC507fco_XKDHWjVbaC3-3" target="jC507fco_XKDHWjVbaC3-5">
          <mxGeometry relative="1" as="geometry">
            <mxPoint x="55" y="105" as="sourcePoint" />
            <Array as="points">
              <mxPoint x="85" y="105" />
            </Array>
          </mxGeometry>
        </mxCell>
        <mxCell id="jC507fco_XKDHWjVbaC3-7" value="" style="shape=umlLifeline;participant=umlActor;perimeter=lifelinePerimeter;whiteSpace=wrap;html=1;container=1;collapsible=0;recursiveResize=0;verticalAlign=top;spacingTop=36;outlineConnect=0;fillColor=#f8cecc;strokeColor=#b85450;" vertex="1" parent="jC507fco_XKDHWjVbaC3-1">
          <mxGeometry x="95" y="80" width="20" height="410" as="geometry" />
        </mxCell>
        <mxCell id="jC507fco_XKDHWjVbaC3-8" value="" style="html=1;points=[];perimeter=orthogonalPerimeter;" vertex="1" parent="jC507fco_XKDHWjVbaC3-7">
          <mxGeometry x="5" y="70" width="10" height="300" as="geometry" />
        </mxCell>
        <mxCell id="jC507fco_XKDHWjVbaC3-9" value="dispatch" style="html=1;verticalAlign=bottom;startArrow=oval;endArrow=block;startSize=8;rounded=0;" edge="1" parent="jC507fco_XKDHWjVbaC3-7" target="jC507fco_XKDHWjVbaC3-8">
          <mxGeometry relative="1" as="geometry">
            <mxPoint x="-55" y="70" as="sourcePoint" />
          </mxGeometry>
        </mxCell>
        <mxCell id="jC507fco_XKDHWjVbaC3-10" value="dispatch" style="html=1;verticalAlign=bottom;endArrow=block;entryX=0;entryY=0;rounded=0;" edge="1" parent="jC507fco_XKDHWjVbaC3-1" source="jC507fco_XKDHWjVbaC3-8" target="jC507fco_XKDHWjVbaC3-4">
          <mxGeometry relative="1" as="geometry">
            <mxPoint x="200" y="160" as="sourcePoint" />
          </mxGeometry>
        </mxCell>
        <mxCell id="jC507fco_XKDHWjVbaC3-11" value="[items=true]" style="edgeLabel;html=1;align=center;verticalAlign=middle;resizable=0;points=[];" vertex="1" connectable="0" parent="jC507fco_XKDHWjVbaC3-10">
          <mxGeometry x="-0.8588" y="2" relative="1" as="geometry">
            <mxPoint x="22" y="-8" as="offset" />
          </mxGeometry>
        </mxCell>
        <mxCell id="jC507fco_XKDHWjVbaC3-12" value="return" style="html=1;verticalAlign=bottom;endArrow=open;dashed=1;endSize=8;exitX=0;exitY=0.95;rounded=0;" edge="1" parent="jC507fco_XKDHWjVbaC3-1" source="jC507fco_XKDHWjVbaC3-4" target="jC507fco_XKDHWjVbaC3-8">
          <mxGeometry relative="1" as="geometry">
            <mxPoint x="200" y="236" as="targetPoint" />
          </mxGeometry>
        </mxCell>
        <mxCell id="jC507fco_XKDHWjVbaC3-13" value=":Object" style="shape=umlLifeline;perimeter=lifelinePerimeter;whiteSpace=wrap;html=1;container=1;collapsible=0;recursiveResize=0;outlineConnect=0;fillColor=#d5e8d4;strokeColor=#82b366;" vertex="1" parent="jC507fco_XKDHWjVbaC3-1">
          <mxGeometry x="490" y="80" width="100" height="400" as="geometry" />
        </mxCell>
        <mxCell id="jC507fco_XKDHWjVbaC3-14" value="" style="html=1;points=[];perimeter=orthogonalPerimeter;" vertex="1" parent="jC507fco_XKDHWjVbaC3-13">
          <mxGeometry x="44" y="200" width="10" height="80" as="geometry" />
        </mxCell>
        <mxCell id="jC507fco_XKDHWjVbaC3-15" value="dispatch" style="html=1;verticalAlign=bottom;endArrow=block;entryX=0;entryY=0;rounded=0;" edge="1" parent="jC507fco_XKDHWjVbaC3-1" source="jC507fco_XKDHWjVbaC3-4" target="jC507fco_XKDHWjVbaC3-14">
          <mxGeometry relative="1" as="geometry">
            <mxPoint x="374" y="280" as="sourcePoint" />
          </mxGeometry>
        </mxCell>
        <mxCell id="jC507fco_XKDHWjVbaC3-16" value="return" style="html=1;verticalAlign=bottom;endArrow=open;dashed=1;endSize=8;exitX=0;exitY=0.95;rounded=0;" edge="1" parent="jC507fco_XKDHWjVbaC3-1" source="jC507fco_XKDHWjVbaC3-14" target="jC507fco_XKDHWjVbaC3-4">
          <mxGeometry relative="1" as="geometry">
            <mxPoint x="680" y="430" as="targetPoint" />
          </mxGeometry>
        </mxCell>
        <mxCell id="jC507fco_XKDHWjVbaC3-17" value="" style="group" vertex="1" connectable="0" parent="jC507fco_XKDHWjVbaC3-1">
          <mxGeometry x="278" y="170" width="334" height="190" as="geometry" />
        </mxCell>
        <mxCell id="jC507fco_XKDHWjVbaC3-18" value="loop" style="shape=umlFrame;whiteSpace=wrap;html=1;fillColor=#ffe6cc;strokeColor=#d79b00;" vertex="1" parent="jC507fco_XKDHWjVbaC3-17">
          <mxGeometry width="334" height="190" as="geometry" />
        </mxCell>
        <mxCell id="jC507fco_XKDHWjVbaC3-19" value="&lt;span style=&quot;font-size: 11px; background-color: rgb(255, 255, 255);&quot;&gt;[items=true]&lt;/span&gt;" style="text;html=1;align=center;verticalAlign=middle;resizable=0;points=[];autosize=1;strokeColor=none;fillColor=none;" vertex="1" parent="jC507fco_XKDHWjVbaC3-17">
          <mxGeometry y="32" width="70" height="20" as="geometry" />
        </mxCell>
        <mxCell id="jC507fco_XKDHWjVbaC3-20" style="rounded=0;orthogonalLoop=1;jettySize=auto;html=1;endArrow=none;endFill=0;dashed=1;fillColor=#ffe6cc;strokeColor=#d79b00;" edge="1" parent="jC507fco_XKDHWjVbaC3-1" source="jC507fco_XKDHWjVbaC3-21" target="jC507fco_XKDHWjVbaC3-18">
          <mxGeometry relative="1" as="geometry" />
        </mxCell>
        <mxCell id="jC507fco_XKDHWjVbaC3-21" value="Only on valid items" style="shape=note2;boundedLbl=1;whiteSpace=wrap;html=1;size=25;verticalAlign=top;align=center;fillColor=#ffe6cc;strokeColor=#d79b00;" vertex="1" parent="jC507fco_XKDHWjVbaC3-1">
          <mxGeometry x="630" y="200" width="120" height="60" as="geometry" />
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
- When the same participant pair exchanges multiple messages, vary routing points or waypoints in addition to the y band.
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
