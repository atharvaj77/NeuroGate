# NeuroGate -> NeuroKernel: Migration Plan

> **Objective**: Transform NeuroGate from a stateless API Gateway into a stateful, agent-aware Operating System for Enterprise AI.

---

## 🏗️ Architecture Overview

The transformation focuses on three new layers on top of the existing Gateway:

1.  **The Immune System (Security Layer)**: Active, semantic defense.
2.  **The Kernel (Agent Layer)**: State management, process control, and memory.
3.  **The Hive Mind (Intelligence Layer)**: Shared learning and neural routing.

---

## 📅 Phase 1: Foundation (The Iron Gate & Immune System)
*Focus: Resilience, Observability, and Active Defense.*

### 1.1 "Unbreakable" Resilience Engine
*   **Module**: `com.neurogate.router.resilience`
*   **Current State**: Basic implementation likely exists.
*   **New Features**:
    *   **HedgingStrategy**: Concurrently call 2+ providers (e.g., Azure OpenAI + AWS Bedrock) and return the first response.
    *   **Adaptive Throttling**: Dynamic rate limits based on upstream latency drift.
    *   **Fallback Logic**: If `gpt-4` fails, auto-downgrade to `gpt-3.5-turbo` or `claude-instant`.

### 1.2 Real-Time Pulse Dashboard
*   **Module**: `com.neurogate.pulse`
*   **Current State**: Exists.
*   **New Features**:
    *   **WebSockets**: Implement `PulseWebSocketHandler` for live streaming metrics (Tokens/sec, Error Rate).
    *   **UI Integration**: Build a "Mission Control" React component in the frontend to visualize this stream.

### 1.3 NeuroGuard Active Defense (v1.0)
*   **Module**: `com.neurogate.vault.neuroguard`
*   **Current State**: `JailbreakDetector`, `PromptInjectionDetector` exist.
*   **Upgrade**:
    *   **Holographic PII**: Add Context-Aware Redaction.
    *   **Model Integration**: Ensure detectors use a local, low-latency SLM (e.g., ONNX-based BERT) rather than regex.

---

## 📅 Phase 2: The Agent Kernel (State & Control)
*Focus: Enabling stateful Agents.*

### 2.1 Durable Agent Memory (The RAM)
*   **Module**: `com.neurogate.agentops.memory` **[NEW]**
*   **Plan**:
    *   **Short-Term Memory (Redis)**: Store conversation context/windows.
    *   **Long-Term Memory (Vector DB)**: Integrate Qdrant/Milvus to store "Episodes" (past successful tool calls).
    *   **API**: `POST /v1/memory/store`, `POST /v1/memory/recall`.

### 2.2 Agent Process Control (The CPU Scheduler)
*   **Module**: `com.neurogate.agentops.control` **[NEW]**
*   **Plan**:
    *   **Loop Detection**: Implement `LoopDetector` to analyze `Trace` objects. If an agent calls the same tool with the same args 5x -> `SIGKILL`.
    *   **Traceability**: Upgrade `TraceContext` to support parent-child relationships for multi-step agent thoughts.

---

## 📅 Phase 3: The Hive Mind (Shared Intelligence)
*Focus: Leveraging data for optimization.*

### 3.1 Neural Routing (v2)
*   **Module**: `com.neurogate.router.intelligence`
*   **Current State**: `ComplexityAnalyzer` (likely static).
*   **Upgrade**:
    *   **Predictive Router**: Train a simple logistic regression model on historical logs (Latency, Token Count, Success) to predict the best model for a given prompt.

### 3.2 Data Flywheel
*   **Module**: `com.neurogate.flywheel`
*   **Plan**:
    *   **Golden Dataset Gen**: Filter logs for "high satisfaction" (user rated or low latency/error).
    *   **Export**: Periodic job to export these rows to `.jsonl` for fine-tuning.

---

## ✅ Success Metrics
*   **Latency Overhead**: < 20ms added by NeuroKernel features.
*   **Safety**: 99.9% block rate on known jailbreaks.
*   **Efficiency**: Reduce token spend by 30% via Semantic Caching and Neural Routing.
