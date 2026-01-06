# Config Module Documentation

The **Config** module centralizes all configuration logic, separating environment-specific properties from business logic and handling dynamic pricing updates.

## Configuration Classes

### `NeuroGateProperties`
**Package:** `com.neurogate.config`

**Purpose:**
A type-safe configuration binding for `application.yml` properties formatted under `neurogate.*`. It organizes config into logical groups (Qdrant, Ollama, Router, RateLimit).

**Key Groups:**
-   `Qdrant`: Host, port, and vector collection settings.
-   `Ollama`: Local LLM endpoint and timeout settings.
-   `Embedding`: Path to the ONNX embedding model and batch sizes.
-   `Router`: Thresholds for complexity analysis and cache enabling flags.

### `RagConfig`
**Package:** `com.neurogate.config`

**Purpose:**
Configuration for the RAG Gateway (Nexus).

**Key Groups:**
- `enabled`: Toggle RAG globally.
- `vectorDb`: Connection details for Qdrant.
- `retrieval`: Default top-k and threshold settings.

### `KafkaConfig`
**Package:** `com.neurogate.config`

**Purpose:**
Configures Apache Kafka beans for async event streaming. It defines the `KafkaTemplate` for producers and initializes topics (e.g., `neurogate-traces`) if they don't exist.

**Key Components:**
-   `traceTopic()`: Bean that creates the `neurogate-traces` topic with configured partitions and replication.

### `PricingConfig`
**Package:** `com.neurogate.config`

**Purpose:**
Manages the cost tables for various LLM providers. In a real production system, this would likely be backed by a dynamic database or external pricing API, but here it provides a configurable in-memory reference.

**Key Methods:**
-   `getProviderCost(provider, model, tokens)`: Calculates the estimated USD cost for a transaction based on token count and model-specific pricing (input vs. output tokens).
