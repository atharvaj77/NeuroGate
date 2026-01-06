# DebugSnapshot

**Package:** `com.neurogate.debugger`

**Purpose:**
Represents the state of an Agent at a single point in time during its execution. Used by the Frontend to render the "Timeline" view.

**Fields:**
- `stepId` (String): Unique identifier for the step (linked to Span ID).
- `timestamp` (Instant): When the step occurred.
- `stepType` (String): Type of action (e.g., `USER_INPUT`, `TOOL_CALL`, `MODEL_RESPONSE`).
- `content` (String): The actual text content (user message, tool arguments, or model reply).
- `state` (Map<String, Object>): Captured memory context or metadata at that moment.

**Usage:**
Populated by `TraceService` when `AIDebuggerService.createSession()` is called.
