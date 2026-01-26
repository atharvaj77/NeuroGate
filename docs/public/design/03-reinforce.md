# Design Document: Reinforce (RLHF & Human Feedback)

**Status**: Implemented  
**Author**: System 2 Agent  
**Date**: January 2026  
**Parent**: [Innovation Roadmap](../INNOVATION.md)

---

## 1. Problem Statement
The "Data Flywheel" (collecting data for fine-tuning) is currently efficient but "dumb".
-   It uses heuristics (e.g., "Was the latency low?") to guess if a trace is good.
-   It cannot capture semantic nuances (e.g., The answer was fast but factually incorrect).
-   True "Golden Data" requires Human Verification.

## 2. Solution: Pulse Reinforce
**Reinforce** is a Human-in-the-Loop (HITL) module. It creates a dedicated workflow for Subject Matter Experts (SMEs) to review, grade, and edit Agent outputs, creating a high-quality dataset for Reinforcement Learning (RLHF) or Supervised Fine-Tuning (SFT).

### Key Value Proposition
> "Tinder for Data Curation."

## 3. Feature Description
1.  **Sampling Strategy**: Not every request needs review. We sample:
    -   1% of all traffic (Random Audit).
    -   100% of "Low Confidence" answers (flagged by `ConsensusService`).
    -   100% of "Negative User Feedback" (User clicked thumbs down).
2.  **Annotation Queue**: A prioritized list of traces waiting for human review.
3.  **Review Interface**:
    -   **Card View**: Query + Agent Answer.
    -   **Actions**: "Approve" (Golden), "Reject" (Bad), or "Rewrite" (Fix it manually).
4.  **Dataset Export**: "Approved" + "Rewritten" traces are exported to `fine-tuning.jsonl`.

## 4. Technical Architecture

### 4.1 Architecture Diagram
`Agent` -> `TraceService` -> (Sampler) -> `ReinforceQueue (Kafka)` -> `Reinforce UI` -> `Golden DB`

### 4.2 Data Model

```java
// Entity: AnnotationTask
class AnnotationTask {
    String traceId;
    String samplerSource; // "RANDOM", "LOW_CONFIDENCE", "USER_FLAG"
    AnnotationStatus status; // PENDING, APPROVED, REJECTED, REWRITTEN
    String humanCorrection; // Null unless REWRITTEN
    String reviewedBy;
    Instant reviewTime;
}
```

### 4.3 Services
1.  **`SamplingService`**:
    -   Interceptors in `TraceService`.
    -   Decides `shouldSample(Trace t)`.
2.  **`AnnotationService`**:
    -   Consumes from Kafka `neurogate-annotations`.
    -   Stores in PostgreSQL `annotation_tasks`.
3.  **`ReinforceController`**:
    -   `GET /api/v1/reinforce/queue`: Fetch pending tasks.
    -   `POST /api/v1/reinforce/{id}/submit`: Submit human judgment.

## 5. Implementation Plan

### Phase 1: The Pipeline
1.  Setup Kafka topic `neurogate-annotations`.
2.  Modify `TraceService.java` to call `SamplingService`.
3.  If sampled, push `traceId` to Kafka.

### Phase 2: The Backend Storage
1.  Create `AnnotationRepository`.
2.  Create the CRUD Controller.
3.  Implement "Rewrite" logic (store the human's text as the new 'ideal' output).

### Phase 3: The UI
1.  Add "Reinforce" tab to the Admin Dashboard.
2.  Implement the "Swipe" interaction (or simple buttons).
3.  Add Keyboard shortcuts (A for Approve, R for Reject) for speed.

## 6. Code Concepts

**SamplingService.java**
```java
@Service
public class SamplingService {
    public boolean shouldSample(Trace trace) {
        // 1. Explicit User Feedback (High Priority)
        if (trace.getUserFeedback() == UserFeedback.THUMBS_DOWN) return true;

        // 2. Low Consensus Score
        if (trace.getMeta().get("consensus_score") < 0.7) return true;

        // 3. Random Sample (1%)
        return random.nextDouble() < 0.01;
    }
}
```

## 7. Testing Strategy
1.  **Sampler Test**:
    -   Mock a trace with `consensus_score = 0.5`. Assert it gets enqueued.
    -   Mock a trace with `consensus_score = 0.9`. Assert it is skipped (mostly).
2.  **Data Integrity**:
    -   Create a task, Rewrite it, Approve it.
    -   Check the Export: It should contain the *Rewritten* text, not the original Agent text.

## 8. Strategic Notes
-   **Data Moat**: This feature allows a startup to build a proprietary dataset that OpenAI/Google *doesn't* have (specific to their domain logic).
-   **Ops Integration**: Can be outsourced to a data labeling team easily.
