# Agent Module Documentation

The **Agent** module provides capabilities for autonomous agent workflows, including memory management and loop detection.

## Safety & Control

### `AgentLoopDetector`
**Package:** `com.neurogate.agent`

**Purpose:**
A safeguard service designed to detect and prevent infinite loops in autonomous agent execution. Agents can sometimes get stuck in repetitive cycles (e.g., repeatedly trying the same failing tool call). This service monitors request patterns to halt such execution.

**Key Methods:**
- `validateRequest(ChatRequest request)`: Checks if the current request is part of a loop by comparing its semantic hash with recent valid requests in the same session.
- `calculateContentHash(String content)`: Generates a SHA-256 hash of the request content (system prompt + user content) to identify duplicates even if metadata differs.

**Mechanism:**
- Maintains a sliding window valid history of the last 5 request hashes per session using a Caffeine cache.
- Throws `AgentLoopException` if 3 consecutive identical requests are detected.

---

## Memory

### `AgentMemoryService`
**Package:** `com.neurogate.agent.memory`

**Purpose:**
Provides long-term memory capabilities for agents, allowing them to store and retrieve information across sessions.

**Key Methods:**
- `save(StoreMemoryRequest request)`: Stores a memory fragment (text) and returns an ID.
- `search(String query, int limit)`: Semantically searches for relevant memories based on a query string.

**Current Status:**
> [!NOTE]
> This service is currently a **Stub Implementation**.
> Future versions will integrate with vector databases (e.g., Qdrant) to provide true semantic search and persistence.
