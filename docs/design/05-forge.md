# Design Document: Forge (Auto-Distillation)

**Status**: Implemented  
**Author**: System 2 Agent  
**Date**: January 2026  
**Parent**: [Innovation Roadmap](../INNOVATION.md)

---

## 1. Problem Statement
Using GPT-4 for everything is expensive and slow.
-   Enterprises want to move to smaller, faster models (e.g., Llama 3 8B, Haiku) for cost/latency reasons.
-   However, small models are "dumber". They fail at complex reasoning.
-   **Fine-tuning** can fix this, but the data collection and training pipeline is manual and hard.

## 2. Solution: Forge
**Forge** is an automated "Distillation Pipeline".
1.  It uses GPT-4 (The Teacher) to handle production traffic and generate high-quality traces.
2.  It filters these traces (using `Cortex` or `Reinforce`) to find the "Golden" ones.
3.  It automatically triggers a fine-tuning job (on OpenAI/Azure/AWS) to train a "Student" model (GPT-4o-mini or Llama).
4.  Once trained, it hot-swaps the model in the Router.

### Key Value Proposition
> "Collapse the cost of Intelligence."

## 3. Feature Description
1.  **Trace Collection**: Leverages the `Flywheel` module.
2.  **Grading & Filtering**: Only keeps traces where `EvaluationScore > 90` or `HumanFeedback == Approved`.
3.  **Job Orchestration**: Connects to OpenAI Fine-tuning API or a GPU provider (Lambda/RunPod) to run the training.
4.  **Model Registry**: Tracks the newly created fine-tuned models (`ft:gpt-3.5:neurogate:exp-1`).

## 4. Technical Architecture

### 4.1 Workflow
```text
[Traffic] -> [GPT-4 (Teacher)] 
               |
               v
           [Trace DB] -> [Forge Filter] -> [JSONL Dataset]
                                             |
                                         [Training Job]
                                             |
           [Router] <---------------- [New Model ID]
```

### 4.2 Components
1.  **`DistillationService`**:
    -   Config: `teacher_model`, `student_base_model`, `trigger_threshold` (e.g., 500 new golden examples).
2.  **`TrainingProvider`**:
    -   `OpenAITrainProvider`: Uses API to start fine-tuning.
    -   `LoRaProvider` (Advanced): Runs a PEFT/LoRA job on a Kubernetes GPU node.

### 4.3 Data Model
```java
class DistillationJob {
    String id;
    String status; // COLLECTING, TRAINING, COMPLETED, FAILED
    int datasetSize;
    String resultingModelId;
    Map<String, Double> evalMetrics; // Training Loss, Validation Loss
}
```

## 5. Implementation Plan

### Phase 1: Dataset Preparation
1.  Create a Scheduled Task (`@Scheduled(cron="0 0 2 * * ?")`) to run nightly.
2.  Query `EvaluationResults` (from Cortex) for high-scoring traces.
3.  Format them into OpenAI's Chat format: `{"messages": [{"role": "user", ...}, {"role": "assistant", ...}]}`.
4.  Upload file to OpenAI Files API.

### Phase 2: Trigger Training
1.  Call `client.fineTuning.jobs.create()`.
2.  Store the `job_id`.
3.  Poll for status completion.

### Phase 3: Deployment
1.  Once `succeeded`, get the `fine_tuned_model` name.
2.  Update NeuroGate's `ProviderConfig` to add this new model as an option.
3.  (Optional) Auto-deploy to a Canary subset of users via `Router`.

## 6. Code Concepts

**DistillationJobService.java**
```java
public void runNightlyCycle() {
    // 1. Gather Data
    var goldenTraces = repo.findGoldenTraces(Since.YESTERDAY);
    if (goldenTraces.size() < 100) return; // Not enough data

    // 2. Upload
    var fileId = openAI.uploadFile(toJsonL(goldenTraces));

    // 3. Train
    var job = openAI.createFineTuneJob(fileId, "gpt-4o-mini");
    
    // 4. Save Record
    db.save(new JobRecord(job.getId(), "STARTED"));
}
```

## 7. Testing Strategy
1.  **Mock Training**:
    -   Don't actually spend $ on training in dev. Mock the OpenAI API response to return a fake `ft:model-id`.
2.  **Data Format**:
    -   Ensure the generated JSONL is strictly valid (no missing fields, correct role names). OpenAI is strict about this.
3.  **Performance**:
    -   Ensure generating the dataset from 10k traces doesn't OOM the application. (Stream processing).

## 8. Strategic Notes
-   **Cost Savings**: This is the strongest ROI argument for NeuroGate. "We lowered your AI bill by 80% by moving from GPT-4 to a fine-tuned Mini model."
-   **Lock-in**: Helps users build assets (models) that are specific to their business, increasing platform loyalty.

## 9. Implementation Reference
See [Forge Class Documentation](../classes/forge.md) for detailed API and class references.
