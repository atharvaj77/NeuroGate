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
    - `play(request)`: Ephemeral execution of a prompt with variables (Playground).
    - `diff(request)`: Computes semantic difference between two prompt texts.
    - `compareShadow(request)`: **[New]** Executes Production vs Shadow versions in parallel and returns comparison.
    - `optimize(request)`: **[New]** Uses Meta-Prompting to rewrite user prompts for better performance.

### `PromptWorkflow`
**Role**: Entity / Redis Hash
- Tracks the deployment state of a logical prompt.
- **Fields**:
    - `activeProductionVersionId`: Version ID currently live in PROD.
    - `activeStagingVersionId`: Version ID currently live in STAGING.
    - `activeProductionVersionId`: Version ID currently live in PROD.
    - `activeStagingVersionId`: Version ID currently live in STAGING.
    - `activeShadowVersionId`: **[New]** Version ID currently live in SHADOW (Specter Mode).
    - `lastDeployedToProduction`: Timestamp of last prod deployment.
    - `lastDeployedToShadow`: Timestamp of last shadow deployment.

### `PromptRegistry`
**Role**: High-Performance Lookup Service
- Serves as the source of truth for "Which version is active?".
- Uses Caffeine cache to store `PromptVersion` objects for O(1) retrieval during inference.
- **Key Methods**:
    - `getProductionPrompt(name)`: Cached lookup for runtime agents.
    - `getProductionPrompt(name)`: Cached lookup for runtime agents.
    - `getShadowPrompt(name)`: **[New]** Cached lookup for shadow (Specter) testing.
    - `promote(name, versionId, env)`: Updates `PromptWorkflow` (supports `prod`, `staging`, `shadow`) and invalidates cache.

### `DiffService`
**Role**: Utility Service
- Computes text differences between two strings for visualization.
- Uses `java-diff-utils` library.
- **Key Methods**:
    - `computeDiff(original, revised)`: Returns a list of `DiffDelta` objects (INSERT, DELETE, CHANGE).

### `OptimizerService`
**Role**: AI Prompt Engineer
- **Purpose**: Uses an LLM (Meta-Prompting) to rewrite and optimize user prompts based on a specific objective (e.g., "Fix Grammar", "Make Concise", "Enhance Reasoning").
- **Key Methods**:
    - `optimize(request)`:
        1.  Constructs a Meta-Prompt containing the user's prompt + objective.
        2.  Calls the smartest available model (e.g., GPT-4).
        3.  Parses the JSON response to extract the optimized text and explanation.
