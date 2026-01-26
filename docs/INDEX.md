# NeuroGate Documentation

Welcome to **NeuroGate** - the Agent-Native AI Gateway and Kernel.

---

## Quick Start

- [Quick Start Guide](./public/QUICKSTART.md) - Deploy in 5 minutes
- [API Documentation](./public/guides/API_DOCUMENTATION.md) - Complete API reference
- [Swagger UI](http://localhost:8080/swagger-ui.html) - Interactive API explorer

---

## Public Documentation

### Core Features
| Module | Description |
|--------|-------------|
| [Multi-Provider Routing](./public/01-router.md) | Intelligent routing with fallbacks |
| [Agent Kernel](./public/02-agentops.md) | Memory, tracing, loop detection |
| [Pulse Observability](./public/03-pulse.md) | Real-time metrics, OpenTelemetry |
| [NeuroGuard Security](./public/04-neuroguard.md) | PII protection, jailbreak detection |
| [Hive Mind Consensus](./public/05-hivemind.md) | Multi-model voting |
| [Time-Travel Debugger](./public/06-debugger.md) | Session replay, semantic diff |

### Architecture Design
| Document | Description |
|----------|-------------|
| [Cortex](./public/design/01-cortex.md) | Evaluation engine |
| [Synapse](./public/design/02-synapse.md) | Prompt studio |
| [Reinforce](./public/design/03-reinforce.md) | HITL feedback |
| [Nexus](./public/design/04-nexus.md) | RAG gateway |
| [Forge](./public/design/05-forge.md) | Auto-distillation |

### API Reference
See [Class Index](./public/classes/INDEX.md) for package documentation.

### Changelog
[Release Notes](./public/CHANGELOG.md)

---

## Internal Documentation

> For team reference only.

| Document | Description |
|----------|-------------|
| [Competitive Analysis](./internal/COMPETITIVE_ANALYSIS.md) | Market analysis, roadmap |
| [Phase 1 Summary](./internal/PHASE1_COMPLETION.md) | OTEL, SDK, API docs |
| [Phase 2 Summary](./internal/PHASE2_COMPLETION.md) | A/B testing, intent routing |
| [Phase 2 Plan](./internal/PHASE2_PLAN.md) | Implementation details |
| [Refactoring](./internal/REFACTORING_OPPORTUNITIES.md) | Technical debt analysis |
| [Testing Guide](./internal/TESTING_CONFIGURATION.md) | Test setup |
| [Migration Plan](./internal/NEUROKERNEL_MIGRATION_PLAN.md) | Architecture evolution |

---

## Getting Started

```bash
# Start infrastructure
docker-compose up -d

# Set API key
export OPENAI_API_KEY="sk-..."

# Run
./gradlew bootRun

# Test
curl http://localhost:8080/v1/health
```

See main [README](../README.md) for complete setup instructions.
