# Sentinel Module Documentation

The **Sentinel** module acts as the gateway and orchestrator for NeuroGate. It handles incoming requests, request validation, logging, and coordinates with other modules (Vault, Router, Agent) to process the AI workload.

## Core API

### `ChatController`
**Package:** `com.neurogate.sentinel.controller`

**Purpose:**
exposure The REST API entry point for NeuroGate, mimicking the OpenAI API specification (`/v1/chat/completions`). It handles HTTP request mapping, validation, and MDC logging setup.

**Key Methods:**
- `createChatCompletion(ChatRequest request, ...)`: Handles standard blocking chat requests. Supports RAG context injection via `rag_enabled`.
- `createStreamingChatCompletion(ChatRequest request)`: Handles SSE (Server-Sent Events) streaming requests.
- `health()`: Simple health check endpoint.

**Context:**
The "front door" of the application. All external traffic hits this class first.

---

## Orchestration

### `SentinelService`
**Package:** `com.neurogate.sentinel`

**Purpose:**
The "Grand Central Station" of NeuroGate. It orchestrates the entire lifecycle of a request, tying together security, routing, and observability.

**Key Responsibilities:**
1.  **Request Validation**: Checks request validity and strictly enforces schemas.
2.  **Safety Checks**: Calls `ActiveDefenseService` to scan for jailbreaks/prompt injection.
3.  **Loop Prevention**: Consults `AgentLoopDetector` to stop recursive agent loops.
4.  **PII Sanitization**: Invokes `PiiSanitizationService` to protect sensitive data before leaving the boundary.
5.  **Routing**: Delegates to `RouterService` or `MultiProviderRouter` to select the backend provider.
6.  **Observability**: Publishes `PulseEvent` metrics for every stage (Received, Sent, Error).

**Key Methods:**
- `processRequest(ChatRequest request)`: Handles the full flow for blocking requests. Captures latency and cache hits.
- `processStreamRequest(ChatRequest request)`: Handles the complex flow for streaming requests, including setting up a `StreamingPiiRestorer` to de-tokenize chunks in real-time as they stream back from the provider.
