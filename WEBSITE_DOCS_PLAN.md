# Website & Documentation Implementation Plan

## 1. Website Overhaul
**Goal**: Rebrand NeuroGate from "AI Gateway" to "Agent-Native Operating System (NeuroKernel)".
*   **[MODIFY]** `website/app/page.tsx`:
    *   **Hero Section**: Change headline to "The Agent-Native Operating System".
    *   **Features Grid**:
        *   Replace "Rate Limiting" with "**Iron Gate: Unbreakable Resilience**" (Hedging, Adaptive Limits).
        *   Replace "Observability" with "**Pulse: Real-Time Holographic Dashboard**".
        *   Add "**NeuroGuard: Active Defense**" (Holographic PII, Jailbreak Protection).
        *   Add "**Hive Mind: Shared Intelligence**" (Neural Routing, Consensus).
    *   **Visuals**: Add "Pulse" heartbeat animation CSS.

## 2. Technical Documentation
**Goal**: Create comprehensive developer guides.
*   **[NEW]** `docs/INDEX.md`: Implementation of the Documentation Index.
*   **[NEW]** `docs/01-router.md`: Documentation for Resilience & Neural Routing (`core.router`).
*   **[NEW]** `docs/02-agentops.md`: Documentation for Memory & Control (`core.agentops`).
*   **[NEW]** `docs/03-pulse.md`: Documentation for Real-Time Dashboard (`core.pulse`).
*   **[NEW]** `docs/04-neuroguard.md`: Documentation for Active Defense (`core.vault`).

## 3. Verification
*   **Manual**: Use `generate_image` to preview UI changes (simulated).
*   **Manual**: Verify markdown rendering for docs.
