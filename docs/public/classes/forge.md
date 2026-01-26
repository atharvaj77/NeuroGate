# Forge Module Documentation

The **Forge** module implements the automated "Distillation Pipeline". It enables NeuroGate to fine-tune smaller, cost-effective models (like GPT-4o-mini) using high-quality traces collected from larger models (like GPT-4).

## Core Service

### `DistillationService`
**Package:** `com.neurogate.forge.service`

**Purpose:**
The orchestrator for the distillation process. It manages the lifecycle of creating fine-tuning jobs, from data collection to training triggering.

**Key Methods:**
- `runNightlyCycle()`: Scheduled task (cron-based) that gathers golden traces, uploads them, and triggers training.
- `triggerManualDistillation()`: Manually triggers the distillation cycle.
- `triggerTraining(List<FeedbackRequest> traces, String baseModel)`: Internal method to execute the training workflow for a given set of traces.

---

## Data Model

### `DistillationJob`
**Package:** `com.neurogate.forge.model`

**Purpose:**
A JPA Entity that represents a single fine-tuning job.

**Key Fields:**
- `jobId`: The external provider's job ID (e.g., `ftjob-xyz`).
- `status`: Current status (`COLLECTING`, `TRAINING`, `COMPLETED`, `FAILED`).
- `datasetSize`: Number of training examples used.
- `resultingModelId`: The ID of the fine-tuned model (once completed).
- `evalMetrics`: Map of loss/accuracy metrics.

### `DistillationJobRepository`
**Package:** `com.neurogate.forge.repository`

**Purpose:**
Spring Data JPA repository for persisting `DistillationJob` entities.

---

## Training Providers

### `TrainingProvider` (Interface)
**Package:** `com.neurogate.forge.provider`

**Purpose:**
Defines the contract for interacting with external fine-tuning services.

**Key Methods:**
- `uploadFile(File file)`: Uploads the JSONL training dataset.
- `startTrainingJob(String fileId, String baseModel)`: URL to trigger the fine-tuning process.
- `getJobStatus(String jobId)`: Polls for job completion status.

### `OpenAITrainingProvider`
**Package:** `com.neurogate.forge.provider`

**Purpose:**
Implementation of `TrainingProvider` for the OpenAI API. Uses `RestClient` to interact with OpenAI's Files and Fine-tuning endpoints.

---

## API & Control

### `ForgeController`
**Package:** `com.neurogate.forge.controller`

**Purpose:**
Exposes REST endpoints to manage and monitor distillation jobs.

**Key Endpoints:**
- `POST /api/v1/forge/jobs/trigger`: Manually triggers a new distillation job.
- `GET /api/v1/forge/jobs`: Lists all historical distillation jobs.
- `GET /api/v1/forge/jobs/{id}`: retrieves details of a specific job.

---

## Configuration

### `ForgeConfig`
**Package:** `com.neurogate.forge.config`

**Purpose:**
Configuration properties for the Forge module.

**Properties:**
- `neurogate.forge.enabled`: Feature toggle.
- `neurogate.forge.teacher-model`: The source model for traces (default: `gpt-4`).
- `neurogate.forge.student-base-model`: The target model for fine-tuning (default: `gpt-4o-mini`).
- `neurogate.forge.trigger-threshold`: Minimum number of golden traces required to start a job.
- `neurogate.forge.schedule-cron`: Cron expression for the nightly cycle.
