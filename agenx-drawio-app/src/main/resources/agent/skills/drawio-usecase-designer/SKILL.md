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

## Avoid

- Do not model a use case diagram as a flowchart.
- Do not place actors inside the system boundary.
- Do not connect actors directly to include or extend edges.
- Do not use `<include>` or `<extend>` as XML label text when `html=1`; they can be parsed as tags and disappear.
- Do not turn low-level implementation steps into separate use cases unless the user explicitly wants that granularity.
- Do not let any use case touch, cross, or sit outside the system boundary.
- Do not shrink the system boundary so tightly that labels or ovals visually collide with the border.
- Do not let multiple actor associations or include/extend edges fully overlap; offset anchors or use waypoints when necessary.
