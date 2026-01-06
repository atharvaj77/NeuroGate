# Consensus Module Documentation

The **Consensus** module (Hive Mind) enables multi-model decision making. It queries multiple LLM providers in parallel and synthesizes their responses into a single, high-confidence answer.

## Services

### `ConsensusService`
**Package:** `com.neurogate.consensus`

**Purpose:**
Orchestrates the "Hive Mind" strategy. It fan-outs the user's prompt to a group of configured models (e.g., GPT-4, Claude 3, Gemini Ultra) and collects their responses.

**Key Methods:**
- `reachConsensus(String prompt, List<Supplier<String>> providers)`: Executes the consensus workflow.
    1.  **Fan-out**: Calls `HedgingService` to query all providers in parallel.
    2.  **Aggregation**: Collects valid responses.
    3.  **Synthesis**: (Future) Uses a judge model to merge the answers. Currently concatenates them for review.

**Context:**
Used for high-stakes queries where accuracy is paramount and cost/latency are secondary.
