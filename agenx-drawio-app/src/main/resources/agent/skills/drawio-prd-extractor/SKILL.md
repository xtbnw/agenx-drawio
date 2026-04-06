---
name: drawio-prd-extractor
description: Extract diagram-worthy structure from PRDs and feature briefs before choosing the right draw.io diagram form.
---

# Draw.io PRD Extractor

Use this skill when the input is a PRD, requirement document, feature brief, long-form product description, or mixed notes that must be distilled into a diagram before rendering.

## Applicable Inputs

- PRD text
- Requirement lists
- User story collections
- Product feature briefs
- Functional design notes

## Input Understanding Rules

- Extract roles, goals, modules, entities, key flows, decisions, constraints, and external dependencies.
- Separate must-have structure from descriptive detail.
- Identify whether the best diagram shape is flowchart, sequence, use case, architecture, or a small combination centered on one dominant type.
- When a specialized diagram type is clear, hand off semantically to that skill's rules.
- When the document is mixed or ambiguous, choose the single clearest diagram that preserves the core intent.

## Output Constraints

- Summarize the PRD into diagram-worthy structure rather than reproducing the document.
- Keep only the nodes, actors, messages, modules, and branches that materially affect understanding.
- Prefer one accurate primary diagram over an overloaded all-in-one drawing.
- In blueprint modes, make extracted roles, flows, modules, and dependencies explicit.
- In XML modes, preserve the distilled semantics and avoid raw text dumping.

## Layout Rules

- Choose the diagram type first, then follow that layout discipline strictly.
- Keep modules or feature groups grouped together when they are central to the PRD.
- Place supporting notes outside the main structure.
- Use whitespace to separate unrelated concerns.

## Node And Edge Semantics

- Roles become actors or participants depending on the chosen diagram type.
- Goals and capabilities become use cases or process nodes depending on the chosen diagram type.
- Key decisions become decision nodes only when they materially branch the behavior.
- Dependencies become edges only when they clarify causality, control flow, or ownership.

## Avoid

- Do not convert every sentence or bullet into a node.
- Do not preserve incidental prose, examples, or repetition as diagram elements.
- Do not mix several incompatible diagram styles unless the request explicitly requires it.
- Do not skip the extraction step and jump straight into raw XML structure.
