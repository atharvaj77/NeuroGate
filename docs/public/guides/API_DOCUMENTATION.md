# NeuroGate API Documentation Guide

This guide covers the API documentation, OpenTelemetry integration, model pricing, and Python SDK generation implemented in Phase 1.

## Swagger UI

NeuroGate provides interactive API documentation via Swagger UI.

### Accessing Swagger UI

```bash
# Start NeuroGate
./gradlew bootRun

# Access Swagger UI
open http://localhost:8080/swagger-ui.html

# Access raw OpenAPI spec
curl http://localhost:8080/api-docs
```

### Available Endpoints

The API is organized into the following groups:

| Tag | Description | Base Path |
|-----|-------------|-----------|
| Chat | OpenAI-compatible completions | `/v1/chat/completions` |
| Models | Model info and pricing | `/v1/models` |
| Analytics | Usage and cost tracking | `/api/v1/analytics` |
| NeuroGuard | Security and PII detection | `/v1/neuroguard` |
| RAG | Retrieval-Augmented Generation | `/api/rag` |
| Cortex | Evaluation engine | `/api/v1/cortex` |
| Forge | Fine-tuning/distillation | `/api/v1/forge` |
| Reinforce | Human feedback | `/api/v1/reinforce` |
| Synapse | Prompt optimization | `/api/v1/synapse` |
| Consensus | Multi-model consensus | `/api/hive` |
| AgentOps | Agent tracing | `/v1/agentops` |
| Memory | Agent memory | `/v1/agent/memory` |
| Flywheel | Feedback collection | `/v1/flywheel` |
| Debugger | Session replay | `/api/debug` |
| Prompts | Prompt versioning | `/api/prompts` |

---

## Model Cost Calculator

NeuroGate includes comprehensive model pricing and cost estimation.

### List Available Models

```bash
# Get all models
curl http://localhost:8080/v1/models

# Filter by provider
curl http://localhost:8080/v1/models?provider=openai
curl http://localhost:8080/v1/models?provider=anthropic
curl http://localhost:8080/v1/models?provider=google
```

**Response:**
```json
{
  "object": "list",
  "data": [
    {
      "id": "gpt-4o",
      "name": "GPT-4o",
      "provider": "openai",
      "inputCostPer1k": 0.0025,
      "outputCostPer1k": 0.01,
      "contextWindow": 128000,
      "maxOutputTokens": 16384,
      "capabilities": ["chat", "function_calling", "vision", "json_mode"],
      "available": true,
      "family": "gpt-4"
    }
  ]
}
```

### Supported Models

| Model ID | Provider | Input $/1K | Output $/1K | Context |
|----------|----------|------------|-------------|---------|
| gpt-4o | OpenAI | $0.0025 | $0.01 | 128K |
| gpt-4o-mini | OpenAI | $0.00015 | $0.0006 | 128K |
| gpt-4-turbo | OpenAI | $0.01 | $0.03 | 128K |
| o1-preview | OpenAI | $0.015 | $0.06 | 128K |
| o1-mini | OpenAI | $0.003 | $0.012 | 128K |
| claude-3-5-sonnet-20241022 | Anthropic | $0.003 | $0.015 | 200K |
| claude-3-5-haiku-20241022 | Anthropic | $0.0008 | $0.004 | 200K |
| claude-3-opus-20240229 | Anthropic | $0.015 | $0.075 | 200K |
| gemini-1.5-pro | Google | $0.00125 | $0.005 | 2M |
| gemini-1.5-flash | Google | $0.000075 | $0.0003 | 1M |
| meta.llama3-1-70b-instruct-v1:0 | Bedrock | $0.00099 | $0.00099 | 128K |
| mistral-large-latest | Mistral | $0.002 | $0.006 | 128K |

### Get Model Pricing

```bash
curl http://localhost:8080/v1/models/gpt-4o/pricing
```

**Response:**
```json
{
  "model": "gpt-4o",
  "provider": "openai",
  "currency": "USD",
  "input_cost_per_1k_tokens": 0.0025,
  "output_cost_per_1k_tokens": 0.01,
  "context_window": 128000,
  "max_output_tokens": 16384
}
```

### Estimate Request Cost

```bash
curl -X POST http://localhost:8080/v1/models/estimate \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-4o",
    "inputText": "What is the capital of France?",
    "expectedOutputTokens": 100
  }'
```

**Response:**
```json
{
  "model": "gpt-4o",
  "estimatedInputTokens": 8,
  "estimatedOutputTokens": 100,
  "inputCost": 0.00002,
  "outputCost": 0.001,
  "totalCost": 0.00102,
  "cacheHitCost": 0,
  "potentialSavings": 0.00102
}
```

### Compare Models

```bash
curl -X POST "http://localhost:8080/v1/models/compare?models=gpt-4o,gpt-4o-mini,claude-3-5-sonnet-20241022" \
  -H "Content-Type: application/json" \
  -d '{
    "inputTokens": 1000,
    "expectedOutputTokens": 500
  }'
```

---

## OpenTelemetry Integration

NeuroGate exports traces to OTLP-compatible backends (Jaeger, Zipkin, Grafana Tempo).

### Configuration

Set environment variables or configure in `application.yml`:

```yaml
# application.yml
management:
  tracing:
    sampling:
      probability: 1.0  # Sample 100% of requests
  otlp:
    tracing:
      endpoint: ${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4317}
```

**Environment Variables:**
```bash
export OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317
export OTEL_SERVICE_NAME=neurogate
export OTEL_TRACES_SAMPLER_ARG=1.0
```

### Starting Jaeger for Local Development

```bash
# Start Jaeger all-in-one
docker run -d --name jaeger \
  -p 16686:16686 \
  -p 4317:4317 \
  -p 4318:4318 \
  jaegertracing/all-in-one:latest

# View traces
open http://localhost:16686
```

### Trace Attributes

NeuroGate traces include LLM-specific semantic conventions:

| Attribute | Description | Example |
|-----------|-------------|---------|
| `gen_ai.system` | AI provider | `openai`, `anthropic` |
| `gen_ai.request.model` | Model name | `gpt-4o` |
| `gen_ai.usage.input_tokens` | Input token count | `150` |
| `gen_ai.usage.output_tokens` | Output token count | `500` |
| `gen_ai.usage.cost_usd` | Request cost | `0.0075` |
| `neurogate.trace_id` | NeuroGate trace ID | `abc123` |
| `neurogate.session_id` | Session ID | `sess_xyz` |
| `neurogate.span_type` | Span type | `LLM_CALL`, `TOOL_CALL` |

### Bridging NeuroGate Traces to OTEL

The `OtelTraceBridge` class converts NeuroGate's AgentOps traces to OpenTelemetry spans:

```java
@Autowired
private OtelTraceBridge otelBridge;

// Export a complete trace
otelBridge.exportTrace(trace);

// Start a real-time span
Span span = otelBridge.startSpan("llm-call", spanId, Context.current());

// Add attributes
otelBridge.addAttributes(spanId, Map.of("model", "gpt-4o"));

// End span
otelBridge.endSpan(spanId);
```

---

## Python SDK Generation

NeuroGate includes OpenAPI Generator configuration for Python SDK generation.

### Generate the SDK

```bash
cd core
./gradlew openApiGenerate
```

The SDK is generated at `core/clients/python/`.

### SDK Structure

```
core/clients/python/
├── neurogate/
│   ├── api/
│   │   ├── chat_api.py
│   │   ├── models_api.py
│   │   ├── analytics_api.py
│   │   └── ...
│   ├── models/
│   │   ├── chat_request.py
│   │   ├── chat_response.py
│   │   └── ...
│   └── __init__.py
├── setup.py
└── README.md
```

### Using the Python SDK

```python
from neurogate import ApiClient, Configuration
from neurogate.api import ChatApi, ModelsApi

# Configure client
config = Configuration(host="http://localhost:8080")
client = ApiClient(config)

# Chat completion
chat_api = ChatApi(client)
response = chat_api.create_chat_completion({
    "model": "gpt-4o",
    "messages": [
        {"role": "user", "content": "Hello!"}
    ]
})
print(response.choices[0].message.content)

# List models
models_api = ModelsApi(client)
models = models_api.list_models()
for model in models.data:
    print(f"{model.id}: ${model.input_cost_per_1k}/1K input")

# Estimate cost
estimate = models_api.estimate_cost({
    "model": "gpt-4o",
    "input_tokens": 1000,
    "expected_output_tokens": 500
})
print(f"Estimated cost: ${estimate.total_cost}")
```

### Installing the SDK

```bash
cd core/clients/python
pip install -e .
```

---

## API Authentication

NeuroGate supports Bearer token authentication:

```bash
curl -X POST http://localhost:8080/v1/chat/completions \
  -H "Authorization: Bearer your-api-key" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-4o",
    "messages": [{"role": "user", "content": "Hello"}]
  }'
```

---

## Custom Headers

NeuroGate supports custom headers for advanced features:

| Header | Description | Example |
|--------|-------------|---------|
| `X-Trace-Id` | Custom trace ID for distributed tracing | `trace_abc123` |
| `X-Session-Id` | Session ID for conversation tracking | `sess_xyz789` |
| `X-Canary-Weight` | Canary weight for A/B testing (0-100) | `10` |

---

## Health Check

```bash
curl http://localhost:8080/v1/health
# Response: "NeuroGate is running"

curl http://localhost:8080/actuator/health
# Response: {"status": "UP"}
```

---

## Prometheus Metrics

NeuroGate exports metrics in Prometheus format:

```bash
curl http://localhost:8080/actuator/prometheus
```

Key metrics:
- `neurogate_cache_hits_total` - Cache hit count
- `neurogate_cache_misses_total` - Cache miss count
- `neurogate_request_latency_seconds` - Request latency histogram
- `neurogate_cost_savings_total` - Total cost saved
- `neurogate_pii_detections_total` - PII detections count
- `neurogate_provider_requests_total` - Requests by provider
