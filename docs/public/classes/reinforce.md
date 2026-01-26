# Reinforce Classes

## `com.neurogate.reinforce.model.AnnotationTask`
**Purpose**: Represents a single human-in-the-loop review task.
**Fields**:
- `traceId`: The trace being reviewed.
- `samplerSource`: Reason for sampling (RANDOM, LOW_CONFIDENCE, USER_FLAG).
- `status`: Current state (PENDING, APPROVED, REJECTED, REWRITTEN).
- `humanCorrection`: The SME's correction if status is REWRITTEN.

## `com.neurogate.reinforce.repository.AnnotationRepository`
**Purpose**: Data access layer for `AnnotationTask`.
**Key Methods**:
- `findByStatus(AnnotationStatus status)`: Get pending tasks.
- `findByTraceId(String traceId)`: Check if task exists.

## `com.neurogate.reinforce.service.SamplingService`
**Purpose**: Rule engine that decides which traces need human attention.
**Key Methods**:
- `shouldSample(Trace trace)`: Boolean logic based on user feedback, consensus scores, and random sampling.

## `com.neurogate.reinforce.service.AnnotationService`
**Purpose**: Manages lifecycle of annotation tasks.
**Key Methods**:
- `createTaskFromTrace(Trace trace, String source)`: Persists a new pending task.
- `reviewTask(Long id, AnnotationStatus status, String correction)`: Updates task with human judgment.

## `com.neurogate.reinforce.controller.ReinforceController`
**Purpose**: REST API for the Reinforce UI.
**Endpoints**:
- `GET /api/v1/reinforce/queue`: Fetch work queue.
- `POST /api/v1/reinforce/{id}/review`: Submit review.

## `com.neurogate.reinforce.worker.ReinforceConsumer`
**Purpose**: Kafka consumer for the `neurogate-annotations` topic.
**Logic**: Listens for sampled traces and delegates to `AnnotationService` to create tasks.
