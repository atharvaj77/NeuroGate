# Design Document: Cortex (Evaluation Engine)

**Status**: Implemented  
**Author**: System 2 Agent  
**Date**: January 2026  
**Parent**: [Innovation Roadmap](../INNOVATION.md)

---

## 1. Problem Statement
In the current LLM landscape, "probabilistic software" creates a reliability gap. Developers can write prompt logic, but ensuring it works consistently across 1,000 edge cases is difficult.
-   **Manual Testing**: Developers manually chat with their agent to "vibe check" it.
-   **Regression**: A prompt change to fix one bug (e.g., tone) often introduces another (e.g., JSON schema violation).
-   **No Metrics**: There is no quantitative score (e.g., "Faithfulness: 85%") to gate deployments.

## 2. Solution: Pulse Cortex
**Cortex** is an integrated Evaluation Engine acting as a CI/CD pipeline for AI Agents. It allows developers to define test suites (Datasets) and run them against their agents using "LLM-as-a-Judge" to score the outputs.

### Key Value Proposition
> "Unit Testing for Soft Logic."

## 3. Feature Description
1.  **Dataset Management**: CRUD operations for `EvaluationDataset` interactively via UI or API.
2.  **Runner Execution**: Parallel execution of an Agent against a dataset.
3.  **Judges**: Pre-configured prompts (or custom ones) that behave as assertions.
    -   `FaithfulnessJudge`: Did the answer come from the context?
    -   `RelevanceJudge`: Did it answer the user's specific question?
    -   `JsonLimitJudge`: Is the output valid JSON and under X tokens?
4.  **Reporting**: A dashboard showing Pass/Fail rates and granular trace scores.

## 4. Technical Architecture

### 4.1 Data Model (New Entities)

```java
// Entity: EvaluationDataset
class EvaluationDataset {
    String id;
    String name;
    List<EvaluationCase> cases;
}

// Entity: EvaluationCase
class EvaluationCase {
    String input;       // "How do I reset my password?"
    String idealOutput; // "Go to settings > security." (Optional)
    Map<String, Object> context; // Metadata
}

// Entity: EvaluationRun
class EvaluationRun {
    String id;
    String datasetId;
    String agentVersion;
    List<EvaluationResult> results;
    Double overallScore;
}

// Entity: EvaluationResult
class EvaluationResult {
    String caseId;
    String agentOutput;
    String judgeReasoning; // "Failed because the tone was rude."
    int score;             // 0-100
}
```

### 4.2 Services

1.  **`CortexService`**: The orchestrator.
    -   `runEvaluation(datasetId, agentId)`: Triggers the workflow.
    -   Uses `HedgingService` (existing) or `CompletableFuture` to run cases in parallel (batch size: 10).
    
2.  **`JudgeService`**: The grader.
    -   Interface `Judge`.
    -   Implementations: `LLMJudge` (uses OpenAI/Gemini), `RegexJudge`, `JsonSchemaJudge`.
    
3.  **`CortexController`**: REST API.
    -   `POST /api/v1/cortex/datasets`: Upload CSV/JSONL.
    -   `POST /api/v1/cortex/runs`: Trigger a run.
    -   `GET /api/v1/cortex/runs/{id}`: Get results.

### 4.3 Database Schema (PostgreSQL/H2)
We need 3 new tables:
-   `cortex_datasets`: Metadata.
-   `cortex_cases`: Inputs and Reference Outputs.
-   `cortex_runs`: Execution history and scores.

## 5. Implementation Plan

### Phase 1: Core Logic (Backend)
1.  **Dependencies**: No new external libs required (Uses existing OpenAI/Gemini clients).
2.  **Domain**: Create `cortex` package in `core`.
3.  **Judges**: Implement `FaithfulnessJudge` prompt.
    ```text
    You are an impartial judge. 
    Question: {input}
    Context: {retrieved_context}
    Answer: {agent_output}
    
    Score the Answer from 0 to 1 on whether it is derived ONLY from Context.
    ```
4.  **Services**: Implement `CortexService` to iterate through cases and call the Agent + Judge.

### Phase 2: API & Storage
1.  Create `EvaluationDatasetRepository`.
2.  Expose endpoints.
3.  Add "Run Cortex" button to the existing Playground.

### Phase 3: Reporting
1.  Update `Pulse` dashboard to show a "Test Stability" chart.

## 6. Code Concepts

**CortexService.java (Snippet)**
```java
public EvaluationReport run(String datasetId) {
    var dataset = repo.findById(datasetId);
    var futures = dataset.getCases().stream()
        .map(c -> CompletableFuture.supplyAsync(() -> evaluateCase(c)))
        .toList();
    
    // Join and aggregate
    return new EvaluationReport(futures);
}

private EvaluationResult evaluateCase(EvaluationCase c) {
    // 1. Run Agent
    var response = agent.chat(c.getInput());
    
    // 2. Run Judge
    var score = judge.grade(c.getInput(), response.getContent(), c.getIdealOutput());
    
    return new EvaluationResult(c, response, score);
}
```

## 7. Testing Strategy

1.  **Unit Test the Judges**:
    -   Create a test where we *know* the answer is hallucinated. Ensure `FaithfulnessJudge` returns 0.
    -   Create a test where the answer is perfect. Ensure `FaithfulnessJudge` returns 1.
2.  **Integration Test**:
    -   End-to-end flow: Upload Dataset -> Run -> Verify Report stored in DB.
3.  **Performance**:
    -   Ensure running 50 cases doesn't time out the original HTTP request (Async execution required).

## 8. Strategic Notes
-   **Resume**: This demonstrates "MLOps" and "AI Engineering" skills, which are rarer than standard "Prompt Engineering".
-   **Future**: We can integrate this with GitHub Actions so a PR *fails* if the generic Agent score drops below 90%.
