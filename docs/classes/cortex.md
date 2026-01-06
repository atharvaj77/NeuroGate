# Cortex Module Documentation

The `cortex` module implements the **Evaluation Engine**, enabling automated "LLM-as-a-Judge" testing for AI agents.

## Core Entities

### [EvaluationDataset](file:///Users/atharva.joshi/PycharmProjects/NeuroGate/core/src/main/java/com/neurogate/core/cortex/EvaluationDataset.java)
**Purpose**: Represents a suite of test cases (a "Dataset").
- **Key Fields**:
    - `id`: UUID.
    - `name`: Human-readable name of the dataset.
    - `cases`: List of `EvaluationCase` items.

### [EvaluationCase](file:///Users/atharva.joshi/PycharmProjects/NeuroGate/core/src/main/java/com/neurogate/core/cortex/EvaluationCase.java)
**Purpose**: Represents a single unit test scenario.
- **Key Fields**:
    - `input`: The prompt/question sent to the agent.
    - `idealOutput`: The expected "Gold Standard" answer.
    - `context`: JSON metadata map for dynamic prompt injection.

### [EvaluationRun](file:///Users/atharva.joshi/PycharmProjects/NeuroGate/core/src/main/java/com/neurogate/core/cortex/EvaluationRun.java)
**Purpose**: Represents a single execution of a Dataset against a specific version of an Agent.
- **Key Fields**:
    - `agentVersion`: The version string of the agent being tested.
    - `overallScore`: The aggregate score (0-100) of all cases in this run.
    - `results`: List of `EvaluationResult` items.

### [EvaluationResult](file:///Users/atharva.joshi/PycharmProjects/NeuroGate/core/src/main/java/com/neurogate/core/cortex/EvaluationResult.java)
**Purpose**: The outcome of running a single `EvaluationCase`.
- **Key Fields**:
    - `agentOutput`: The actual response received from the agent.
    - `score`: Integrity score (0-100) assigned by the Judge.
    - `judgeReasoning`: Explanation from the LLM Judge for the score.

## Services & Logic

### [CortexService](file:///Users/atharva.joshi/PycharmProjects/NeuroGate/core/src/main/java/com/neurogate/core/cortex/CortexService.java)
**Title**: The Evaluation Orchestrator
**Purpose**: Manages the lifecycle of datasets and executes evaluation runs.
- **Key Responsibilities**:
    - **Dataset Management**: CRUD operations for Datasets and Cases.
    - **Parallel Execution**: Uses `CompletableFuture` to run multiple test cases concurrently against the Judge to reduce latency.
    - **Scoring Aggregation**: Calculates the average score for an `EvaluationRun`.

### [Judge](file:///Users/atharva.joshi/PycharmProjects/NeuroGate/core/src/main/java/com/neurogate/core/cortex/Judge.java)
**Purpose**: Functional interface for evaluation logic.
- **Method**: `grade(input, output, idealOutput)` returns a score (0.0 - 1.0).

### [FaithfulnessJudge](file:///Users/atharva.joshi/PycharmProjects/NeuroGate/core/src/main/java/com/neurogate/core/cortex/FaithfulnessJudge.java)
**Purpose**: Implementation of `Judge` using Spring AI.
- **Logic**: Prompts an LLM (e.g., GPT-4/Gemini) to act as an impartial judge, comparing the Agent's answer to the Ideal Answer and Context.
- **Output**: Returns a numeric faithfulness score.

## API Layer

### [CortexController](file:///Users/atharva.joshi/PycharmProjects/NeuroGate/core/src/main/java/com/neurogate/core/cortex/CortexController.java)
**Purpose**: REST API endpoints for the Frontend/CLI.
- `POST /api/v1/cortex/datasets`: Create new test suites.
- `POST /api/v1/cortex/runs`: Trigger an evaluation run.
- `GET /api/v1/cortex/runs/{id}`: Retrieve run results.
