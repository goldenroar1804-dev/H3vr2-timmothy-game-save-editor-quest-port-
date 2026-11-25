# Claude Task Assignment

## Status
**Awaiting Claude's first audit**

## Current task
Perform an **independent read-only audit** of the Timmy Quest Editor repository. Do not modify source files or commit changes in this task.

### Examine
- Android file picker / SAF and folder permission persistence.
- Recursive scanning and file filtering.
- JSON parsing, GZIP handling and raw editor flow.
- Direct save-back behavior and backup safety.
- Dynamic field/path editing, including nested objects and arrays.
- Build/workflow configuration and potential compilation errors.

### Requirements
- Do not assume or invent the actual H3VR2 save schema.
- Do not claim tests/builds passed unless you have run them.
- Identify specific bugs with file paths and line references where possible.
- Prioritize findings by severity.
- Suggest small, targeted fixes rather than rewriting the project.

## Deliverable
Provide an audit report in your Claude task response and, if possible, a draft PR containing documentation-only findings. Do not merge anything.

## Coordination
Read AI_COORDINATION.md. ChatGPT will handle the primary build verification and code fixes after reviewing this audit.
