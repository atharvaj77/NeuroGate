# NeuroGate Documentation

Welcome to the official documentation for **NeuroGate**, the Agent-Native Operating System.

## Architecture Modules

### [1. Synapse: Visual Prompt Studio](./classes/synapse.md)
*   **Version Control**: Semantic hashing and Git-like workflow for prompts.
*   **Specter Mode**: Shadow deployments for risk-free testing in production.
*   **Start-of-the-Art Optimization**: AI-driven prompt rewriting.

### [2. Cortex: Evaluation Engine](./classes/cortex.md)
*   **LLM-as-a-Judge**: Automated quality assurance for agent outputs.
*   **Ad-Hoc Evaluation**: Rapid, transient testing of prompt performance.

### [3. Iron Gate: Resilience & Routing](./01-router.md)
*   **Neural Routing**: Score-based backend selection.
*   **Hedging**: Scatter-gather concurrency for reliability.
*   **Adaptive Rate Limiting**: Dynamic throttling based on upstream latency.

### [2. Agent Kernel: Memory & Control](./02-agentops.md)
*   **Durable Memory**: Redis-backed short-term context with compression.
*   **Process Control**: Loop detection and Hallucination Guards.
*   **Tracing**: Distributed trace capture for agent workflows.

### [3. Pulse: Observability](./03-pulse.md)
*   **Real-Time Dashboard**: WebSocket streaming of metric events.
*   **Holographic Metrics**: Latency, Token Usage, and Cost tracking per provider.

### [4. NeuroGuard: Active Defense](./04-neuroguard.md)
*   **PII Vision**: Context-aware redaction (SSN, API Keys).
*   **Jailbreak Detection**: Protection against prompt injection.
*   **Reversible Tokenization**: Secure data handling pipeline.


### [5. Hive Mind: Shared Intelligence](./05-hivemind.md)
*   **Consensus**: Multi-model voting and synthesis.
*   **Data Flywheel**: Automatic "Golden Trace" extraction for fine-tuning.

### [6. Time Travel Debugger](./06-debugger.md)
*   **Holographic Replay**: Step-through debugging of agent traces.
*   **Forking**: Branch sessions to test variations.
*   **State Inspection**: View memory context at each execution step.

## Project Meta
- [Changelog](./CHANGELOG.md): History of changes and updates.
- [Branding Guidelines](./BRANDING_GUIDELINES.md): Terminology and positioning rules for contributors.

## Getting Started
See [README.md](../README.md) for deployment instructions.
