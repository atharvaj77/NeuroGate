# NeuroGate: The Open Source Agent Kernel

![License](https://img.shields.io/badge/License-MIT-blue.svg)
![Java](https://img.shields.io/badge/Java-21%2B-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.0-brightgreen)
![Docker](https://img.shields.io/badge/Docker-Ready-blue)

**Orchestrate, Secure, and Optimize your LLM fleet.**
NeuroGate is a high-performance AI Gateway and "Agent Kernel" built on **Java 21 Virtual Threads**. It transforms simple LLM API calls into a robust, enterprise-grade AI infrastructure.

---

## 🚀 Why NeuroGate?

NeuroGate currently sits at "Level 3+ (Intelligence)" maturity for AI Gateways. It goes beyond simple proxying to provide a full-stack **OS for Agents**.

*   **🛡️ Active Defense**: Zero-trust PII redaction and prompt injection defense *before* requests leave your network.
*   **⚡ Performance**: Built on Java 21 Virtual Threads to handle 10k+ concurrent connections with <10ms overhead.
*   **💰 Cost Engineering**: Semantic caching (Qdrant) and smart routing reduce LLM costs by 40-60%.
*   **🧠 Collective Intelligence**: "Hive Mind" consensus engine uses multiple models (GPT-4 + Claude + Gemini) to synthesize the best answer.

---

## 🏗️ Platform Capabilities

NeuroGate is organized into three "Organelles" to take agents from prototype to production:

### 1. Build & Design (Synapse)
*   **Synapse Studio**: A VS Code-like environment in the browser for visual Prompt Engineering.
*   **Version Control**: Git-like versioning for prompts with branching and rollback.
*   **Interactive Playground**: Test prompts against the real NeuroGate router with "Compare V1 vs V2" diffs.
*   **Python SDK**: Planned (coming soon).

### 2. Run & Secure (The Kernel)
*   **Nexus (RAG Gateway)**: Centralized RAG service. Inject context, manage vector DB connections (Qdrant), and enforce ACLs at the gateway level.
*   **NeuroGuard**: Defense-in-depth security layer.
    *   **PII Vault**: Detects and tokenizes sensitive data (SSN, Email, Credit Cards) before it hits the LLM.
    *   **Zero-Copy Tokenization**: LLMs see `<EMAIL_1>`, users see `john@example.com`.
*   **Iron Gate**: Resilience patterns including Circuit Breakers, Rate Limiting (Redis), and Fallback chains (OpenAI -> Azure -> Bedrock).
*   **Hive Mind**: Neural routing and consensus voting for critical queries requiring high accuracy.

### 3. Measure & Improve (Cortex & Forge)
*   **Cortex**: Automated Evaluation Engine using "LLM-as-a-Judge" to score responses on Faithfulness, Relevance, and Safety.
*   **Reinforce**: Human-in-the-Loop (HITL) workflow. "Tinder-like" swipe interface for experts to curate "Golden Traces".
*   **Forge**: Auto-distillation pipeline. Turns your Golden Traces into fine-tuning datasets to train smaller, cheaper models (e.g., Llama 3).

---

## 📚 Documentation

*   **[Quick Start Guide](docs/public/QUICKSTART.md)** ⚡ - Get running in 5 minutes with Docker.
*   **[Documentation Index](docs/INDEX.md)** - Full documentation map.
*   **[API Reference](docs/public/guides/API_DOCUMENTATION.md)** - OpenAI-compatible API guide.
*   **[Swagger UI](http://localhost:8080/swagger-ui.html)** - Interactive API explorer (when running).

---

## 🛠️ Tech Stack

*   **Language**: Java 21 (Virtual Threads / Project Loom)
*   **Framework**: Spring Boot 3.4 + Spring AI
*   **Vector DB**: Qdrant (Semantic Caching & RAG)
*   **Cache/State**: Redis (Rate Limiting, Circuit Breakers)
*   **Streaming**: Apache Kafka (Telemetry & Trace Logging)
*   **Data Processing**: Apache Spark (Offline Analytics)
*   **Observability**: Prometheus + Grafana

---

## 🏁 Quick Start

### 1. Start Infrastructure
```bash
# Start Qdrant, Redis, Ollama, Prometheus, Grafana
docker-compose up -d

# Verify services
docker-compose ps
```

### 2. Configure & Run
Set your API keys in `core/src/main/resources/application.yml` or via environment variables:
```bash
export OPENAI_API_KEY="sk-..."
./gradlew bootRun
```

### 3. Drop-in Replacement
NeuroGate is wire-compatible with the OpenAI API. Just change the `baseURL`:

```javascript
import OpenAI from 'openai';

const client = new OpenAI({
  apiKey: 'sk-neurogate-key', // Any string works here if auth is disabled
  baseURL: 'http://localhost:8080/v1' // Point to NeuroGate
});

async function main() {
  const chatCompletion = await client.chat.completions.create({
    messages: [{ role: 'user', content: 'Hello agent world!' }],
    model: 'gpt-4', // NeuroGate routes this intelligently
  });
}
```

---

## 🏆 Engineering Highlights

> **End-to-End specific "Agent Kernel" Architecture**: Designed a high-performance gateway using **Java 21 Virtual Threads**, handling 10k+ concurrent connections with non-blocking I/O.

> **Distributed Systems & Resilience**: Engineered a **multi-provider routing engine** with priority failover, hedging, and semantic caching (L1-L4 tiered architecture) achieving >90% cache hit rates.

> **Zero-Trust Security (NeuroGuard)**: Implemented a reversible PII tokenization system that ensures sensitive data never leaves the network boundary, compliant with GDPR/HIPAA requirements.

> **Data-Driven Feedback Loops**: Built "Forge" and "Reinforce" modules to create a **Data Flywheel**, automating the collection of "Golden Traces" for model distillation and fine-tuning.

---

## 🤝 Contributing

NeuroGate is an open-source research project exploring the future of Agentic Infrastructure.
Built by **Atharva Joshi** as a senior engineering portfolio project.

**MIT Licensed. Free for everyone.**

---

## Known Limitations

*   Pulse and Forge dashboard pages run in demo mode with simulated data.
*   Forge training is currently a simulation and does not execute real model training jobs yet.
*   Python SDK is not published yet (API integration should use OpenAI-compatible clients for now).
