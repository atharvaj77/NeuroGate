# Synapse Module Documentation

The `synapse` module provides the backend logic for the Visual Prompt Studio, enabling version control, semantic diffing, and safe deployment of prompts.

## `com.neurogate.synapse`

### `SynapseController`
**Role**: API Layer
- Exposes endpoints for the Synapse Studio UI.
- **Methods**:
    - `getWorkflow(promptName)`: Returns active production/staging versions.
    - `deploy(request)`: Promotes a version to a target environment.
    - `play(request)`: Ephemeral execution of a prompt with variables (Playground).
    - `diff(request)`: Computes semantic difference between two prompt texts.

### `PromptWorkflow`
**Role**: Entity / Redis Hash
- Tracks the deployment state of a logical prompt.
- **Fields**:
    - `activeProductionVersionId`: Version ID currently live in PROD.
    - `activeStagingVersionId`: Version ID currently live in STAGING.
    - `lastDeployedToProduction`: Timestamp of last prod deployment.

### `PromptRegistry`
**Role**: High-Performance Lookup Service
- Serves as the source of truth for "Which version is active?".
- Uses Caffeine cache to store `PromptVersion` objects for O(1) retrieval during inference.
- **Key Methods**:
    - `getProductionPrompt(name)`: Cached lookup for runtime agents.
    - `promote(name, versionId, env)`: Updates `PromptWorkflow` and invalidates cache.

### `DiffService`
**Role**: Utility Service
- Computes text differences between two strings for visualization.
- Uses `java-diff-utils` library.
- **Key Methods**:
    - `computeDiff(original, revised)`: Returns a list of `DiffDelta` objects (INSERT, DELETE, CHANGE).
