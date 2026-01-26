# AgentOps Module Documentation

The **AgentOps** module provides comprehensive observability and control for AI agent workflows. It enables tracing of multi-step agent executions, detecting issues like infinite loops and hallucinations, and maintaining a historical record of all agent interactions.

## Core Tracing

### `TraceService`
**Package:** `com.neurogate.agentops`

**Purpose:**
Manages the lifecycle of distributed traces for agent workflows. It handles the storage and retrieval of trace data, allowing developers to inspect the full execution path of an agent.

**Key Methods:**
- `saveTrace(Trace trace)`: Persists a complete trace with all its spans.
- `getTrace(String traceId)`: Retrieves a specific trace by ID.
- `getTracesBySession(String sessionId)`: Returns all traces associated with a user session.
- `getStatistics()`: Aggregates metrics like total traces, spans, token usage, and cost.

**Context:**
Used by the `SentinelService` and `Agent` components to record execution steps. It acts as the backend for the observability dashboard and provides the source data for the **Time Travel Debugger**. It now asynchronously publishes all traces to a **Kafka** topic (`neurogate-traces`) for zero-latency decoupling from the analytics pipeline.

---

## Control & Safety

### `LoopDetector`
**Package:** `com.neurogate.agentops.control`

**Purpose:**
A specialized component for detecting circular dependencies or repetitive actions in agent workflows. Unlike the simpler `AgentLoopDetector` in the agent module, this class analyzes the semantic meaning of actions to catch subtler loops.

**Key Methods:**
- `detectLoop(List<Span> spans)`: Analyzes a list of execution spans to identify repetitive patterns.

### `ToolHallucinationGuard`
**Package:** `com.neurogate.agentops.control`

**Purpose:**
Prevents agents from hallucinating tool calls (i.e., calling functions that don't exist or using invalid parameters). It validates tool invocations against a strict schema before execution.

**Key Methods:**
- `validateToolCall(ToolCall call, ToolSchema schema)`: Checks if a generated tool call matches the defined schema.

---

## Data Model

### `TraceContext`
**Package:** `com.neurogate.agentops`

**Purpose:**
Holds thread-local context information for the current trace. Similar to MDC (Mapped Diagnostic Context), it allows trace IDs and span IDs to be propagated across different services without explicit parameter passing.

**Key Methods:**
- `setTraceId(String traceId)`: Sets the current trace ID.
- `getTraceId()`: Retrieves the current trace ID.
- `clear()`: Cleans up the context after request processing.
