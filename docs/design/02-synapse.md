# Design Document: Synapse (Visual Prompt Studio)

**Status**: Implemented  
**Author**: System 2 Agent  
**Date**: January 2026  
**Parent**: [Innovation Roadmap](../INNOVATION.md)

---

## 1. Problem Statement
Currently, NeuroGate's prompt management is "Code-First".
-   Prompts are likely stored in `resources/prompts` or database strings.
-   Editing a prompt requires a code deployment or a raw SQL update.
-   There is no way to visualize version history or "diff" two prompts to see what changed.
-   Non-technical stakeholders (PMs, Domain Experts) cannot edit prompts.

## 2. Solution: Synapse Studio
**Synapse** is a browser-based Integrated Development Environment (IDE) specifically for Prompt Engineering. It sits on top of the existing `PromptVersionControlService` but adds a rich visual layer.

### Key Value Proposition
> "Figma for Prompts."

## 3. Feature Description
1.  **Visual Editor**: A monaco-editor based text area with syntax highlighting for variables (e.g., `{{ customer_name }}`).
2.  **Version Tree**: A Git-graph visualization showing the lineage of prompts (v1 -> v1.1 -> v2).
3.  **Side-by-Side Diff**: Compare Version A vs Version B. Highlight added/removed words.
4.  **Instant Playground**: A "Play" button right next to the editor to test the prompt with mock variables.
5.  **Deployment**: "Promote to Production" button that updates the pointer in the Router.

## 4. Technical Architecture

### 4.1 Data Model (Enhancements)
We leverage the existing `PromptVersionControlService`.

```java
// Existing Concept
class PromptVersion {
    String id; // Hash
    String content;
    String parentId;
    List<String> inputVariables; // ["name", "date"]
}

// Synapse additions
class PromptWorkflow {
    String id;
    String activeProductionVersionId;
    String activeStagingVersionId;
}
```

### 4.2 Frontend Components (React/Next.js)
1.  **`PromptEditor`**:
    -   Uses `react-monaco-editor`.
    -   Custom tokenizer to color `{{...}}` in orange.
2.  **`VariableForm`**:
    -   Auto-generates input fields based on the detected variables in the editor.
3.  **`VersionGraph`**:
    -   Uses `react-flow` or D3 to draw the commit tree.

### 4.3 API Layer (`SynapseController`)
-   `GET /api/v1/synapse/prompts/{name}/versions`: Get tree.
-   `POST /api/v1/synapse/play`: Ephemeral run (does not commit).
    -   Input: `prompt_content`, `variables`, `model`.
    -   Output: `llm_response`.
-   `POST /api/v1/synapse/deploy`: Updates the alias (prod/staging).

## 5. Implementation Plan

### Phase 1: The Editor (Frontend Focus)
1.  Build the Next.js page `/synapse`.
2.  Implement the dual-pane layout: Editor on Left, Preview/Variables on Right.
3.  Connect "Play" button to a generic `Router.generate()` endpoint, passing the raw prompt string.

### Phase 2: Versioning Backend
1.  Solidify `PromptVersionControlService`.
2.  Implement "Semantic Hashing" (SHA-256 of the content) to detect duplicates.
3.  Implement `getDiff(vA, vB)` logic (using a java-diff-utils library).

### Phase 3: Deployment Logic
1.  Create a `PromptRegistry` in Redis/Caffeine.
2.  When "Deploy" is clicked, update the Redis key `prompt:support-bot:production` -> `hash_v2`.
3.  All Agents read from this Registry.

## 6. Code Concepts

**PromptRegistry.java**
```java
@Service
public class PromptRegistry {
    // fast lookups
    private final Cache<String, PromptTemplate> productionPrompts = Caffeine.newBuilder().build();

    public PromptTemplate get(String promptName) {
        return productionPrompts.get(promptName, k -> loadFromDb(k));
    }

    public void promote(String promptName, String versionId) {
        var tmpl = repo.findById(versionId);
        productionPrompts.put(promptName, tmpl);
        notifyCluster(promptName); // PubSub update
    }
}
```

## 7. Testing Strategy

1.  **Frontend Test**:
    -   Verify typing `{{ name }}` creates an input field in the Variable Form.
2.  **Backend Test**:
    -   Verify that switching the production pointer instantly changes the behavior of the `ChatController` for the next request.
3.  **Load Test**:
    -   Ensure the `PromptRegistry` lookups are O(1) and don't slow down the main path.

## 8. Strategic Notes
-   **User Experience**: This makes the platform look "Enterprise Ready" in demos.
-   **Collaboration**: Allows separating the "Prompt Engineer" role from the "Backend Engineer" role.
