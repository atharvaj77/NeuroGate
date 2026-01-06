# Nexus (RAG) Module Documentation

The **Nexus** module implements the RAG Gateway pattern, intercepting requests to inject relevant context from vector databases.

## Core Services

### `NexusService`
**Package:** `com.neurogate.rag.service`

**Purpose:**
Coordinates the RAG workflow: embedding, retrieval, and context injection.

**Key Methods:**
- `enrichRequest(ChatRequest request, String userId)`: Main entry point. Embeds query, searches vector DB, and injects context into the system prompt.

### `RagInterceptor`
**Package:** `com.neurogate.rag.interceptor`

**Purpose:**
 middleware that intercepts incoming `ChatRequest`s. If `rag_enabled` is true, it invokes `NexusService` to enrich the request before it reaches the Router.

## Components

### `EmbeddingService`
- **Impl:** `OpenAiEmbeddingService`
- **Purpose:** Converts text queries into vector embeddings (using OpenAI `text-embedding-3-small` by default).

### `VectorStoreClient`
- **Impl:** `QdrantVectorStoreClient`
- **Purpose:** Abstraction for Vector Database operations. Supports Qdrant via gRPC.
- **Features:**
    - `search`: Semantic search with cosine distance.
    - `upsert`: Indexing documents.
    - `createCollection`: Schema management.

### `RagConfig`
**Package:** `com.neurogate.core.config`
Configuration properties under `nexus.*`:
- `nexus.enabled`: Global toggle.
- `nexus.vector-db.url`: Qdrant URL.
- `nexus.retrieval.top-k`: Default number of documents to retrieve.

## Data Models
- **`ChatRequest`**: Added `ragEnabled` (bool) and `ragOptions` (overrides for topK, threshold).
- **`ChatResponse`**: Added `x_neurogate_citations` and `x_neurogate_similarity` metadata.
