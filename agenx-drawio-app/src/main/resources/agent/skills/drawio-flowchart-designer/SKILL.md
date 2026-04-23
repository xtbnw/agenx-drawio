---
name: drawio-flowchart-designer
description: Specialized guidance for flowcharts, business process diagrams, and step-by-step decision flows.
---

# Draw.io Flowchart Designer

Use this skill when the user explicitly asks for a flowchart, business process diagram, decision flow, login flow, approval flow, or any request with a dominant step-by-step process.

## Applicable Inputs

- Business workflows
- User operation flows
- Approval and exception handling flows
- Backend processing pipelines with decisions and retries

## Input Understanding Rules

- Identify one primary flow axis first. Default to top-to-bottom.
- Separate main path, success path, failure path, retry path, and notes.
- Normalize the process into start, process, decision, database, note, container, and end semantics.
- If the user provides PRD or long prose, extract only the actors, goals, key steps, branches, and dependencies needed for the diagram.

## Output Constraints

- Prefer a single dominant reading direction.
- Keep the main path on the center axis when possible.
- Use explicit branch labels such as yes, no, success, fail, retry only when they add meaning.
- Keep the graph compact but do not trade away readability.
- In blueprint modes, focus on node semantics, branch semantics, and relative placement.

## Layout Rules

- Place start and end on the main axis.
- Place sequential process nodes on aligned rows or columns.
- Put decision nodes on the main axis and place branch targets to the left or right, not both mixed randomly.
- Place failure or retry branches outside the main path and route return edges around the outer side.
- Use containers only when they clarify ownership, stage, or subsystem boundaries.
- Leave enough whitespace so branch edges do not run through process nodes.
- Separate connectors with distinct sides or elbow routes when multiple edges leave or enter the same node.

## Node And Edge Semantics

- `start` and `end` are reserved for lifecycle boundaries.
- `process` is the default for actionable steps.
- `decision` is only for true branching conditions.
- `database` is only for storage or data retrieval points.
- Use labeled edges for conditional branches and retries.
- Model loops explicitly, but keep the loop edge outside the primary axis when possible.

## Avoid

- Do not create multiple competing main axes.
- Do not turn a decision into a normal process box.
- Do not route retry or failure edges through the center of the main flow.
- Do not expand every sentence from a PRD into a separate node.
- Do not let yes/no, retry, or exception branches fully overlap each other for long distances.
