# Design Document: Nexus (RAG Gateway)

**Status**: Implemented  
**Author**: System 2 Agent  
**Date**: January 2026  
**Parent**: [Innovation Roadmap](../INNOVATION.md)

---

## 1. Problem Statement
Retrieval-Augmented Generation (RAG) is the standard for enterprise AI. However, every microservice currently implements its own RAG logic:
-   Connects to Vector DB.
-   Embeds the query.
-   Formats the prompt context.
This leads to fragmentation, duplicated logic, and inconsistent security (e.g., Service A checks document permissions, Service B forgets).

## 2. Solution: Nexus Gateway
**Nexus** moves the Retrieval step *into the Gateway*. The Agent/Client simply sends the user query. The Gateway:
1.  Intercepts the request.
2.  Embeds the query (using a local or API embedder).
3.  Queries a centralized Vector Database (Qdrant/Pinecone).
4.  Filters results based on the User's JWT permissions (ACL).
5.  Injects the retrieved context into the Prompt.
6.  Forwards the enriched prompt to the LLM.

### Key Value Proposition
> "RAG as a Service, at the Edge."

## 3. Feature Description
1.  **Context Injection**: Automatically appends relevant knowledge to the `system` message.
2.  **Centralized ACL**: "User X can only see documents from Department Y." Enforced at the gateway level.
3.  **Hybrid Search**: Supports Keyword + Vector search (Reciprocal Rank Fusion).
4.  **Citation Tracking**: Returns the used document IDs in the response metadata.

## 4. Technical Architecture

### 4.1 Flow
```text
Client -> [Nexus Gateway] -> [Embedding Service]
                          -> [Vector DB (Qdrant)]
                          -> [Context Builder]
                          -> [LLM Provider]
       <- [Response + Citations]
```

### 4.2 Configuration
We define RAG sources in generic config or DB.

```yaml
nexus:
  enabled: true
  embedding_model: "text-embedding-3-small"
  vector_db:
    type: "qdrant"
    url: "localhost:6333"
    collection: "enterprise_docs"
  retrieval:
    top_k: 5
    threshold: 0.75
    acl_field: "department_id"
```

### 4.3 Services
1.  **`VectorStoreClient`**: Abstraction over Qdrant/Pinecone/Milvus.
2.  **`EmbeddingProvider`**: Adapters for OpenAI/Cohere/Local(ONNX).
3.  **`ContextInjector`**: Modifies the `ChatRequest` object.
    -   Finds the `System` message.
    -   Appends `\n\nCONTEXT:\n...`

## 5. Implementation Plan

### Phase 1: Connection & Embedding
1.  Import `qdrant-client` (java).
2.  Implement `EmbeddingService` in `core.rag`.
3.  Create a standard CRUD service to Index documents (so we can test retrieval).

### Phase 2: The Interceptor
1.  Create `RagInterceptor` in the Request Pipeline (before Routing).
2.  Logic:
    -   If `request.metadata.use_rag == true`:
    -   Embed Query.
    -   Search Vector DB.
    -   Rewrite Request.

### Phase 3: Permissions (ACL)
1.  Extract `department_id` or `user_id` from the incoming JWT.
2.  Pass a Filter object to Qdrant: `Filter(must=[FieldCondition(key="department", match=jwt.dept)])`.
3.  Ensure users never retrieve restricted docs.

## 6. Code Concepts

**RagInterceptor.java**
```java
public ChatRequest intercept(ChatRequest req) {
    if (!req.isRagEnabled()) return req;

    // 1. Embed
    float[] vector = embedder.embed(req.getLastUserMessage());

    // 2. Retrieve with ACL
    var userId = authContext.getUserId();
    List<Document> docs = vectorStore.search(vector, 5, userId);

    // 3. Inject
    String contextBlock = formatDocs(docs);
    req.addSystemMessage("Use this context:\n" + contextBlock);

    return req;
}
```

## 7. Testing Strategy
1.  **Retrieval Quality**:
    -   Index 5 distinct documents.
    -   Query for a specific fact in Document A.
    -   Assert Document A is in the returned list.
2.  **Security (ACL)**:
    -   User A (Engineering) queries "Salary info".
    -   Assert that HR documents are *excluded* from the retrieval set.
3.  **Latency**:
    -   Measure overhead. Embedding + Vector Search should be < 200ms.

## 8. Strategic Notes
-   **Enterprise Requirement**: Security (ACL) in RAG is the #1 blocker for enterprise adoption. Nexus solves this centrally.
-   **Simplification**: Application developers don't need to know how to use Qdrant. They just set `rag: true`.
