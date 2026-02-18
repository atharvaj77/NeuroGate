# Engram — Platform-Level Vectorized Data Store Service

> **Module**: `com.neurogate.engram`
> **Status**: Design Phase
> **Owner**: Atharva Joshi
> **Date**: February 2026

---

## 1. Overview

Engram is NeuroGate's centralized Vectorized Data Store Service, providing enterprise-grade ingestion, indexing, retrieval, and metadata management of embeddings at the platform level. It extends NeuroGate's existing Qdrant integration (used by Nexus for RAG) into a first-class, multi-tenant vector platform with configurable collections, streaming ingestion, and full compliance controls.

### Naming Rationale

An **engram** is a unit of cognitive information stored in the brain — the physical trace of memory. This aligns with NeuroGate's neuro-themed module naming and accurately describes the service's role: storing and retrieving the "memories" (embeddings) that power AI applications.

### Relationship to Nexus

| Aspect | Nexus (RAG Gateway) | Engram (Vector Store) |
|--------|---------------------|----------------------|
| Scope | Document-oriented RAG pipeline | Generic vector storage platform |
| Focus | Chunking, retrieval-augmented generation | Collection management, ingestion, search |
| Users | End-users querying documents | Platform teams managing embedding infrastructure |
| Abstraction | High-level (upload doc → search) | Low-level (manage vectors, metadata, indexes) |

Nexus becomes a **consumer** of Engram. Engram is the storage layer; Nexus is the RAG application layer on top.

---

## 2. Architecture

```
┌──────────────────────────────────────────────────────────────────────┐
│                         Engram Vector Store                          │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────────┐   │
│  │  Collection   │  │  Ingestion   │  │      Retrieval           │   │
│  │  Manager      │  │  Pipeline    │  │      Engine              │   │
│  │              │  │              │  │                          │   │
│  │  • CRUD       │  │  • REST API  │  │  • Top-K Search         │   │
│  │  • Schema     │  │  • SDK       │  │  • Metadata Filtering   │   │
│  │  • Versioning │  │  • Kafka     │  │  • Hybrid (Vec+KW)      │   │
│  │  • Metrics    │  │  • Batch     │  │  • Threshold Config     │   │
│  └──────┬───────┘  └──────┬───────┘  └──────────┬───────────────┘   │
│         │                  │                      │                   │
│  ┌──────┴──────────────────┴──────────────────────┴───────────────┐  │
│  │                    Storage Abstraction Layer                    │  │
│  │  • Qdrant (primary)  • pgvector (fallback)  • Sharding Logic  │  │
│  └────────────────────────────────────────────────────────────────┘  │
│                                                                      │
│  ┌───────────────────┐  ┌───────────────────┐  ┌─────────────────┐  │
│  │   Security        │  │   Observability   │  │   Compliance    │  │
│  │   • RBAC          │  │   • Pulse Integ.  │  │   • SOC 2       │  │
│  │   • Encryption    │  │   • Metrics       │  │   • HIPAA       │  │
│  │   • Audit Log     │  │   • Dashboards    │  │   • FedRAMP     │  │
│  └───────────────────┘  └───────────────────┘  │   • GDPR        │  │
│                                                  └─────────────────┘  │
└──────────────────────────────────────────────────────────────────────┘
```

---

## 3. Functional Requirements

### 3.1 Collection Management

Collections are the top-level organizational unit for embeddings. Each collection has a fixed schema and configuration.

```java
@Entity
@Table(name = "engram_collections")
public class EngramCollection {
    @Id
    private UUID id;
    private String name;                    // e.g., "product-embeddings-v2"
    private String organizationId;          // Multi-tenant isolation
    private int dimension;                  // e.g., 1536 (OpenAI), 768 (BERT)
    private SimilarityMetric metric;        // COSINE, DOT_PRODUCT, EUCLIDEAN
    private JsonNode metadataSchema;        // JSON Schema for metadata validation
    private long vectorCount;               // Current vector count
    private int version;                    // Schema version
    private CollectionStatus status;        // ACTIVE, REINDEXING, ARCHIVED
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

**API Endpoints:**

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/engram/collections` | Create collection with schema |
| `GET` | `/api/v1/engram/collections` | List collections (paginated, filtered by org) |
| `GET` | `/api/v1/engram/collections/{id}` | Get collection details + stats |
| `PUT` | `/api/v1/engram/collections/{id}` | Update collection config (non-breaking) |
| `DELETE` | `/api/v1/engram/collections/{id}` | Soft-delete collection |
| `POST` | `/api/v1/engram/collections/{id}/reindex` | Trigger re-indexing |

**Configuration Parameters:**

```json
{
  "name": "product-embeddings-v2",
  "dimension": 1536,
  "metric": "cosine",
  "metadataSchema": {
    "type": "object",
    "properties": {
      "category": { "type": "string", "enum": ["electronics", "clothing", "food"] },
      "price": { "type": "number" },
      "in_stock": { "type": "boolean" }
    },
    "required": ["category"]
  },
  "replication": {
    "factor": 2,
    "regions": ["us-east-1", "us-west-2"]
  },
  "indexing": {
    "hnsw_m": 16,
    "hnsw_ef_construct": 128
  }
}
```

### 3.2 Ingestion Pipeline

Three ingestion modes: real-time API, SDK, and streaming.

**REST API Ingestion:**

```
POST /api/v1/engram/collections/{id}/vectors
Content-Type: application/json

{
  "vectors": [
    {
      "id": "vec-001",
      "embedding": [0.1, 0.2, ...],   // dimension must match collection
      "metadata": { "category": "electronics", "price": 299.99 },
      "content": "Original text (optional, for hybrid search)"
    }
  ],
  "options": {
    "upsert": true,                     // Insert or update
    "validate_metadata": true           // Validate against schema
  }
}
```

**Batch Ingestion (up to 10,000 vectors per request):**

```
POST /api/v1/engram/collections/{id}/vectors/batch
Content-Type: application/x-ndjson

{"id":"v1","embedding":[...],"metadata":{...}}
{"id":"v2","embedding":[...],"metadata":{...}}
```

**Kafka Streaming Pipeline:**

```java
@KafkaListener(topics = "engram.ingest.${collection.id}")
public void ingestFromStream(ConsumerRecord<String, EngramVector> record) {
    engramService.upsert(record.value());
}
```

**SDK Ingestion (Python):**

```python
from neurogate import NeuroGate

client = NeuroGate(api_key="ng_live_xxx")

# Single vector
client.engram.upsert(
    collection="product-embeddings-v2",
    vectors=[{
        "id": "vec-001",
        "embedding": model.encode("wireless headphones"),
        "metadata": {"category": "electronics", "price": 299.99}
    }]
)

# Batch from file
client.engram.batch_upsert(
    collection="product-embeddings-v2",
    file="embeddings.jsonl",
    batch_size=1000
)
```

**Re-indexing:**

```
POST /api/v1/engram/collections/{id}/reindex
{
  "new_dimension": 3072,           // If embedding model changed
  "new_metric": "dot_product",     // If metric changed
  "embedding_model": "text-embedding-3-large",
  "background": true               // Non-blocking re-index
}
```

**Versioning:**

Each vector upsert is versioned. Collections maintain a version counter. Clients can query at a specific version for point-in-time retrieval.

```
GET /api/v1/engram/collections/{id}/vectors?version=42
```

### 3.3 Retrieval Engine

**Top-K Similarity Search:**

```
POST /api/v1/engram/collections/{id}/search
{
  "query_vector": [0.1, 0.2, ...],
  "top_k": 10,
  "threshold": 0.75,                // Minimum similarity score
  "filters": {
    "must": [
      { "field": "category", "match": "electronics" },
      { "field": "price", "range": { "lte": 500 } }
    ],
    "must_not": [
      { "field": "in_stock", "match": false }
    ]
  },
  "include_metadata": true,
  "include_vectors": false
}
```

**Response:**

```json
{
  "results": [
    {
      "id": "vec-001",
      "score": 0.94,
      "metadata": { "category": "electronics", "price": 299.99, "in_stock": true },
      "content": "Wireless noise-cancelling headphones..."
    }
  ],
  "search_time_ms": 12,
  "total_scanned": 150000
}
```

**Hybrid Search (Vector + Keyword):**

```
POST /api/v1/engram/collections/{id}/search/hybrid
{
  "query_vector": [0.1, 0.2, ...],
  "query_text": "wireless headphones",
  "vector_weight": 0.7,
  "keyword_weight": 0.3,
  "top_k": 10
}
```

### 3.4 Observability

Engram integrates with NeuroGate's Pulse monitoring stack.

**Metrics Exposed:**

| Metric | Type | Description |
|--------|------|-------------|
| `engram.ingestion.count` | Counter | Total vectors ingested |
| `engram.ingestion.latency` | Histogram | Ingestion latency (ms) |
| `engram.ingestion.errors` | Counter | Ingestion failures |
| `engram.search.count` | Counter | Total search queries |
| `engram.search.latency` | Histogram | Search latency (ms) |
| `engram.search.results_avg` | Gauge | Average results per query |
| `engram.collection.vector_count` | Gauge | Vectors per collection |
| `engram.collection.storage_bytes` | Gauge | Storage per collection |
| `engram.reindex.progress` | Gauge | Re-indexing progress (0-100%) |

**Dashboard Widgets (Pulse Integration):**

- Ingestion throughput (vectors/sec) over time
- Search latency histogram (P50, P95, P99)
- Collection size growth trend
- Top collections by query volume
- Error rate by operation type

**Usage Reporting:**

```
GET /api/v1/engram/usage?org={orgId}&from=2026-01-01&to=2026-02-01
{
  "period": "2026-01",
  "vectors_stored": 45000000,
  "vectors_ingested": 2300000,
  "searches_performed": 890000,
  "storage_gb": 12.4,
  "compute_hours": 340
}
```

### 3.5 Security & Compliance

**RBAC Per Collection:**

| Role | Create | Read | Write | Delete | Admin |
|------|--------|------|-------|--------|-------|
| Viewer | - | Y | - | - | - |
| Editor | - | Y | Y | - | - |
| Manager | Y | Y | Y | Y | - |
| Admin | Y | Y | Y | Y | Y |

**Encryption:**

- **In Transit**: TLS 1.3 for all API and inter-node communication
- **At Rest**: AES-256-GCM encryption for vector data and metadata
- Per-organization encryption keys managed via AWS KMS
- Key rotation every 90 days (configurable)

**Compliance Controls:**

| Standard | Controls |
|----------|----------|
| SOC 2 Type II | Audit logging, access controls, encryption, monitoring |
| HIPAA | BAA support, PHI isolation, minimum necessary access, audit trail |
| FedRAMP | Gov-cloud deployment option, FIPS 140-2 encryption, continuous monitoring |
| GDPR | Data residency (EU collections stay in EU), right to deletion, DPA |

**Audit Logging:**

Every operation is logged to an immutable audit trail:

```json
{
  "timestamp": "2026-02-18T10:30:00Z",
  "actor": "user:atharva@example.com",
  "action": "VECTOR_SEARCH",
  "resource": "collection:product-embeddings-v2",
  "organization": "org_abc123",
  "ip": "10.0.1.42",
  "result": "SUCCESS",
  "details": { "top_k": 10, "results_returned": 8, "latency_ms": 14 }
}
```

---

## 4. Non-Functional Requirements

### 4.1 Scalability

**Target**: >100 million vectors with <100ms retrieval latency.

**Strategy:**

```
┌─────────────────────────────────────────────────────────┐
│                   Engram Cluster                         │
│                                                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │
│  │  Shard 1     │  │  Shard 2     │  │  Shard N     │    │
│  │  0-33M vecs  │  │  33M-66M     │  │  66M-100M    │    │
│  │  ┌─────────┐ │  │  ┌─────────┐ │  │  ┌─────────┐ │    │
│  │  │ Primary  │ │  │  │ Primary  │ │  │  │ Primary  │ │    │
│  │  └─────────┘ │  │  └─────────┘ │  │  └─────────┘ │    │
│  │  ┌─────────┐ │  │  ┌─────────┐ │  │  ┌─────────┐ │    │
│  │  │ Replica  │ │  │  │ Replica  │ │  │  │ Replica  │ │    │
│  │  └─────────┘ │  │  └─────────┘ │  │  └─────────┘ │    │
│  └─────────────┘  └─────────────┘  └─────────────┘    │
│                                                         │
│  Load Balancer → Route by collection hash               │
│  Auto-shard at 30M vectors per shard                    │
└─────────────────────────────────────────────────────────┘
```

- **Sharding**: Hash-based collection sharding across Qdrant nodes
- **Auto-scaling**: New shards created when a shard exceeds 30M vectors
- **Read replicas**: Each shard has 1+ replica for read scaling
- **Caching**: L1 Caffeine cache for hot queries, L2 Redis for warm

### 4.2 Availability

**Target**: >= 99.9% uptime with multi-region redundancy.

- **Multi-region**: Active-passive across 2+ AWS regions (us-east-1 primary, us-west-2 failover)
- **Failover**: Automatic DNS failover via Route 53 health checks (<60s)
- **Replication**: Asynchronous cross-region replication with <5s lag
- **Backups**: Hourly snapshots to S3 with 30-day retention
- **Zero-downtime deploys**: Rolling updates with health-check gates

### 4.3 Performance Targets

| Operation | P50 | P95 | P99 |
|-----------|-----|-----|-----|
| Single vector upsert | <5ms | <15ms | <50ms |
| Batch upsert (1000 vectors) | <200ms | <500ms | <1s |
| Top-K search (K=10, 10M vectors) | <20ms | <50ms | <100ms |
| Top-K search (K=10, 100M vectors) | <50ms | <80ms | <100ms |
| Filtered search (100M vectors) | <60ms | <90ms | <150ms |
| Collection create | <100ms | <200ms | <500ms |

### 4.4 Integration

- **Platform Observability**: Prometheus metrics, Grafana dashboards, Pulse integration
- **Logging**: Structured JSON logs → ELK/CloudWatch
- **Cost Monitoring**: Per-collection cost tracking via Pulse cost dashboard
- **Alerting**: PagerDuty/Slack integration for latency/error/capacity alerts

---

## 5. Java Package Structure

```
com.neurogate.engram/
├── EngramAutoConfiguration.java          // Spring Boot auto-config
├── config/
│   ├── EngramProperties.java             // Configuration properties
│   └── EngramSecurityConfig.java         // RBAC configuration
├── model/
│   ├── EngramCollection.java             // Collection entity
│   ├── EngramVector.java                 // Vector entity
│   ├── SimilarityMetric.java             // Enum: COSINE, DOT_PRODUCT, EUCLIDEAN
│   ├── CollectionStatus.java             // Enum: ACTIVE, REINDEXING, ARCHIVED
│   └── SearchRequest.java               // Search query model
├── service/
│   ├── CollectionService.java            // Collection CRUD
│   ├── IngestionService.java             // Vector upsert/batch/stream
│   ├── SearchService.java               // Top-K and hybrid search
│   ├── ReindexService.java              // Background re-indexing
│   ├── MetadataValidationService.java   // JSON Schema validation
│   └── EngramMetricsService.java        // Observability metrics
├── controller/
│   ├── CollectionController.java         // REST endpoints
│   ├── VectorController.java            // Ingestion endpoints
│   └── SearchController.java            // Search endpoints
├── repository/
│   ├── CollectionRepository.java         // JPA repository
│   └── VectorStoreAdapter.java          // Qdrant/pgvector abstraction
├── pipeline/
│   ├── KafkaIngestionListener.java      // Kafka consumer
│   └── BatchIngestionProcessor.java     // Batch processing logic
└── security/
    ├── CollectionRbacService.java        // Per-collection RBAC
    └── EngramAuditService.java          // Audit logging
```

---

## 6. SDK Extensions

### Python SDK

```python
from neurogate import NeuroGate

client = NeuroGate(api_key="ng_live_xxx")

# Collection management
collection = client.engram.create_collection(
    name="product-embeddings",
    dimension=1536,
    metric="cosine",
    metadata_schema={...}
)

# Ingest
client.engram.upsert(
    collection="product-embeddings",
    vectors=[{"id": "v1", "embedding": [...], "metadata": {...}}]
)

# Search
results = client.engram.search(
    collection="product-embeddings",
    query_vector=[...],
    top_k=10,
    filters={"must": [{"field": "category", "match": "electronics"}]}
)

# Usage
usage = client.engram.usage(period="2026-02")
```

### Java SDK

```java
EngramClient engram = neurogate.engram();

// Create collection
engram.createCollection(CollectionConfig.builder()
    .name("product-embeddings")
    .dimension(1536)
    .metric(SimilarityMetric.COSINE)
    .build());

// Search
SearchResult result = engram.search("product-embeddings",
    SearchRequest.builder()
        .queryVector(embedding)
        .topK(10)
        .filter(Filter.must("category", "electronics"))
        .build());
```

---

## 7. Migration Path from Nexus

Existing Nexus users who interact with Qdrant directly should be able to migrate:

1. **Phase 1**: Engram wraps existing Qdrant collections, Nexus continues working unchanged
2. **Phase 2**: Nexus refactored to use Engram as its storage layer
3. **Phase 3**: Direct Qdrant access deprecated in favor of Engram APIs

```
Before:  Nexus → Qdrant (direct)
After:   Nexus → Engram → Qdrant (abstracted)
```

This allows Engram to support pgvector or other backends in the future without Nexus changes.

---

## 8. Deployment Architecture (AWS)

```
┌─────────────────────────────────────────────────────────────────┐
│  us-east-1 (Primary)                                            │
│                                                                  │
│  ┌───────────┐    ┌──────────────┐    ┌──────────────────┐     │
│  │  ALB       │───→│  ECS/EKS     │───→│  Qdrant Cluster  │     │
│  │  (TLS 1.3) │    │  (Engram     │    │  (3 nodes,       │     │
│  └───────────┘    │   Service)    │    │   replication=2) │     │
│                    └──────────────┘    └──────────────────┘     │
│                           │                     │                │
│                    ┌──────┴──────┐    ┌─────────┴────────┐     │
│                    │  RDS Postgres│    │  ElastiCache     │     │
│                    │  (metadata)  │    │  (Redis, caching)│     │
│                    └─────────────┘    └──────────────────┘     │
│                           │                                      │
│                    ┌──────┴──────┐                               │
│                    │  MSK (Kafka) │ ← Streaming ingestion        │
│                    └─────────────┘                               │
│                           │                                      │
│                    ┌──────┴──────┐                               │
│                    │  S3 (backups │                               │
│                    │  + snapshots)│                               │
│                    └─────────────┘                               │
├─────────────────────────────────────────────────────────────────┤
│  Cross-Region Replication → us-west-2 (Failover)               │
└─────────────────────────────────────────────────────────────────┘
```

---

## 9. Implementation Timeline

| Week | Focus | Deliverables |
|------|-------|-------------|
| 9 | Collection Management & Foundation | Collection CRUD, schema validation, Qdrant abstraction |
| 10 | Ingestion Pipeline | REST/batch/Kafka ingestion, versioning, re-indexing |
| 11 | Retrieval & Search | Top-K search, metadata filtering, hybrid search, perf optimization |
| 12 | Security, Compliance & Observability | RBAC, encryption, audit logging, Pulse dashboards, AWS multi-region |

---

*This design document defines the Engram module within the NeuroGate ecosystem. It should be reviewed alongside the weekly implementation plans in `docs/week/week9-12/`.*