# Hive Mind: Shared Intelligence

The `core.consensus` and `core.flywheel` packages implement collective intelligence.

## Consensus (The Council)
For high-stakes queries, we don't trust a single model.
- **Service**: `ConsensusService.java`
- **Workflow**:
    1.  User asks a question.
    2.  System queries OpenAI, Anthropic, and Gemini in parallel (`HedgingService`).
    3.  `ConsensusAggregator` synthesis the 3 answers into a final, higher-confidence response.

## Data Flywheel (Evolution)
The system learns from its own operation.
- **Quality Filter**: `QualityFilter.java` identifies "Golden Traces" (Fast, Tool-Using, Error-Free).
- **Exporter**: `FlywheelExporter.java` runs nightly to export these traces to JSONL.
- **Goal**: Fine-tune a smaller, cheaper model (e.g., Llama 3) to mimic the behavior of the Consensus engine.

### Analytics Engine
We now use **Apache Spark** for large-scale processing of agent traces.
- **Script**: [`spark/flywheel_analytics.py`](../spark/flywheel_analytics.py)
- **Infrastructure**: **Apache Kafka** decouples trace ingestion (`TraceService` -> `neurogate-traces` topic) from the request path.
