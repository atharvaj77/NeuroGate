# Router Module Documentation

The **Router** module is the brain of NeuroGate, responsible for intelligently directing LLM requests to the optimal provider. It handles failover, load balancing, and "Neural Routing" based on performance scores and prompt complexity.

## Core Routing

### `MultiProviderRouter`
**Package:** `com.neurogate.router.provider`

**Purpose:**
The primary service for routing requests. It implements the "Neural Router" pattern, dynamically selecting providers based on availability, priority, and neural scores.

**Key Methods:**
- `route(ChatRequest request)`: Main entry point. Attempts to route to the primary provider for the requested model, with automatic failover to equivalent models on other providers (e.g., GPT-4 -> Claude 3 Opus).
- `routeStream(ChatRequest request)`: Handles streaming requests with similar failover logic.
- `getProvidersStatus()`: Returns current health and metadata for all configured providers.

**Context:**
The central hub for all outgoing LLM traffic. It orchestrates `ResilienceService` for circuit breaking and `NeuralRouteStrategy` for smart selection.

---

### `LLMProvider` (Interface)
**Package:** `com.neurogate.router.provider`

**Purpose:**
Defines the standard contract that all AI model providers must implement. This abstraction allows NeuroGate to be provider-agnostic.

**Key Methods:**
- `generate(ChatRequest request)`: Synchronous chat completion.
- `generateStream(ChatRequest request)`: Streaming chat completion (Flux).
- `isAvailable()`: Health check.
- `getEquivalentModel(String model)`: Maps a generic or competitor's model name to this provider's closest equivalent.

**Implementations:**
- `OpenAiProvider`
- `AnthropicProvider`
- `GeminiProvider`
- `AzureOpenAiProvider`
- `OllamaProvider` (Local LLM support)
- `BedrockProvider` (AWS Bedrock)

---

## Intelligence & Scoring

### `NeuralRouteStrategy`
**Package:** `com.neurogate.router.neural`

**Purpose:**
Implements the selection logic for the "Neural Router". It uses real-time scores to rank and select the best providers for a given task.

**Key Methods:**
- `selectBestProviders(List<String> candidates, int n)`: Returns the top N providers from a list of candidates, sorted by their current neural score.

---

### `ProviderScoreService`
**Package:** `com.neurogate.router.neural`

**Purpose:**
Calculates and maintains a live "Neural Score" (0.0 to 1.0) for each provider. The score is a composite metric based on latency, error rate, and cost.

**Key Methods:**
- `updateScores()`: Scheduled task that recalculates scores every 30 seconds.
- `getScore(String provider)`: Returns the current score for a provider.

**Scoring Factors:**
- **Latency (40%)**: Lower is better.
- **Error Rate (40%)**: Stability is critical.
- **Cost (20%)**: Preference for cheaper models when performance is comparable.

---

### `ComplexityAnalyzer`
**Package:** `com.neurogate.router.intelligence`

**Purpose:**
Analyzes incoming user prompts to determine their complexity across multiple dimensions (Reasoning, Domain Knowledge, Output Length, Creativity). This analysis helps route simple queries to faster/cheaper models and complex queries to more capable ones.

**Key Methods:**
- `analyze(String prompt)`: validat Returns a `ComplexityScore` object with ratings (1-10) for each dimension.

**Current Implementation:**
Uses heuristic analysis (keyword matching, pattern recognition).
- **Reasoning**: Looks for words like "analyze", "explain", "code".
- **Domain**: Detects technical jargon (medical, legal).
- **Length**: Infers expected response size.
- **Creativity**: Identifies creative writing tasks.

**Future Roadmap:**
Plans to upgrade to a fine-tuned DistilBERT classifier for higher accuracy.
