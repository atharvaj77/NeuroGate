# Prompts Module Documentation

The **Prompts** module provides a full Git-like version control system for LLM prompts, including branching, merging, rollback, and A/B testing.

## Core Service

### `PromptVersionControlService`
**Package:** `com.neurogate.prompts`

**Purpose:**
The engine behind prompt management. It treats prompts like code, using semantic hashing to detect changes and managing a history of versions.

**Key Features:**
1.  **Semantic Versioning**: Automatically calculates Major/Minor/Patch version increments based on the semantic similarity (embedding distance) between the new prompt and its parent.
    -   > 95% similarity: Patch increment.
    -   60-95% similarity: Minor increment.
    -   < 60% similarity: Major increment (breaking change).
2.  **Git-like Operations**: Supports distinct branches (e.g., `main`, `dev`, `experiment-A`) and merging with conflict detection.
3.  **A/B Testing**: Can execute a controlled test where traffic is split between two prompt versions, collecting metrics to determine a winner.

**Key Methods:**
- `commit(...)`: Saves a new version of a prompt.
- `createBranch(...)`: Forks a lineage from a specific version.
- `mergeBranches(...)`: Merges a feature branch back into main, checking for semantic conflicts.
- `runABTest(...)`: Runs `N` requests, splitting traffic between `Version A` and `Version B`, and returns a statistical comparison of cost, latency, and success rate.

### `PromptController`
**Package:** `com.neurogate.prompts`

**Purpose:**
REST API for interacting with the version control system. Used by the frontend "Prompt Studio".

**Endpoints:**
-   `POST /commit`: Save changes.
-   `POST /ab-test`: specialized endpoint for triggering an A/B test run.
-   `POST /rollback`: Revert to a previous "safe" version.
