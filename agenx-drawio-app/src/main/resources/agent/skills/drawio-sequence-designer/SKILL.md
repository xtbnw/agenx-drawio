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

## Layout Rules

- Place participants left to right across the top.
- Place each participant's timeline vertically below it.
- Put messages on separate horizontal bands ordered by time.
- Keep participant lanes clearly separated, typically with at least 220px horizontal spacing.
- Keep message rows clearly separated, typically with at least 80px vertical spacing, and use more when labels are long.
- Keep participant headers at one top row and do not place later interaction blocks back into the same top area.
- Increase page height for long conversations instead of compressing rows until they overlap.
- Keep return messages visually distinct only when they carry useful meaning.
- Group optional or related interactions with notes or containers only when they improve readability.

## Node And Edge Semantics

- Participants are actors, users, systems, services, databases, or external integrations.
- Messages represent ordered interactions, not generic dependencies.
- Returns should point back to the caller.
- Self calls are allowed only for internal processing on the same participant.
- Activation or focus should follow message handling rather than arbitrary decoration.

## Avoid

- Do not place sequence steps on a single process axis like a flowchart.
- Do not mix message order with structural dependency order.
- Do not omit sender or receiver identity.
- Do not turn include, extend, or business rules into separate participants.
- Do not stack multiple stages into the same vertical region so that earlier and later interactions overlap.
- Do not place participant headers, notes, and message blocks on top of each other to save space.
