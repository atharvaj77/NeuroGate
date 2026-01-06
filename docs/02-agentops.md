# Agent Kernel: Memory & Control

The `core.agentops` package enables stateful, safe agent execution.

## Agent Memory
Agents need persistent context across REST calls.
- **Store**: `RedisMemoryStore.java` (Redis Lists).
- **Compression**: `MemoryCompressor.java` automatically summarizes conversation history when it exceeds token limits, ensuring the "Context Window" never overflows.

## Process Control
Agents can get stuck in loops or hallucinate tool names.
- **Loop Prevention**: `LoopDetector.java` analyzes the Trace history for repeated subsequences (e.g., 3x `web_search` with same query).
- **Hallucination Guard**: `ToolHallucinationGuard.java` uses Levenshtein distance to auto-correct typos in tool calls (e.g., `web_serch` -> `web_search`).

## Tracing
Every action is recorded as a `Span` within a `Trace`.
- **Model**: `Trace.java`
- **Fields**: Input, Output, Tools Used, Latency, Cost.
