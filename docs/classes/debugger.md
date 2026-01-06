# Debugger Module Documentation

The **Debugger** module (AI Debugger) is an advanced tool for inspecting, replaying, and analyzing LLM interactions. It allows developers to "time travel" by replaying past requests with modified parameters to understand non-deterministic behavior.

## Core Service

### `AIDebuggerService`
**Package:** `com.neurogate.debugger`

**Purpose:**
The backend engine for the AI Debugger. It records every request/response pair and provides APIs to replay them, compare variations, and inspect internal state (embeddings, tokens).

**Key Features:**
1.  **Time Travel**: Replay any past request with different models or temperature settings.
2.  **Semantic Diffing**: Compare two responses not just by text, but by semantic similarity using embeddings.
3.  **Variable Inspection**: View the exact PII tokens and embedding vectors generated during a request.

**Key Methods:**
- `recordRequest(...)`: Asynchronously saves a debug record for every processed request.
- `createSession(String requestId)`: Initializes a debugging session for a specific past request (populates `snapshots` from Trace).
- `forkSession(String sessionId, String stepId, Map<String, Object> modifications)`: Creates a new session branched from a specific step.
- `replay(String sessionId, ReplayOptions options)`: Re-executes the request with optional overrides (e.g., forcing a cache miss or changing the model).
- `compareResponses(String sessionId)`: Calculates the semantic similarity (cosine similarity) and text diff between the original and replayed response.
- `searchRecords(DebugSearchFilter filter)`: Advanced search for past requests by latency, cost, or provider.

**Context:**
Powered by `SemanticCacheService` and `RouterService`. It is a critical tool for prompt engineering and QA.
