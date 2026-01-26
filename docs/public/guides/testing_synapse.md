# Synapse Features Testing Guide

This guide provides step-by-step instructions to verify the "Synapse" features of NeuroGate: Version Control, Interactive Playground, Diff, and Promotion.

## Prerequisites

*   **NeuroGate Core** must be running locally on port `8080`.
*   A tool to open HTTP requests (`curl`, Postman, or Insomnia).

---

## 1. Version Control (Git-like)

Verify the ability to commit prompt changes and manage branches.

### A. Commit a New Prompt
Create a new prompt version (or the first version).

**Endpoint:** `POST /api/prompts/commit`

```bash
curl -X POST http://localhost:8080/api/prompts/commit \
  -H "Content-Type: application/json" \
  -d '{
    "promptText": "You are a helpful assistant named {{bot_name}}.",
    "commitMessage": "Initial commit",
    "author": "tester",
    "branchName": "main"
}'
```

**Expected Output:** JSON response containing a `versionId` (e.g., UUID) and the saved prompt data.

### B. View Version History
See the list of commits for a branch.

**Endpoint:** `GET /api/prompts/versions?branchName=main`

```bash
curl "http://localhost:8080/api/prompts/versions?branchName=main"
```

**Expected Output:** A JSON array of version objects.

### C. Create a Branch
Create a new feature branch from an existing version. Replace `<VERSION_ID>` with the ID from Step A.

**Endpoint:** `POST /api/prompts/branches`

```bash
curl -X POST http://localhost:8080/api/prompts/branches \
  -H "Content-Type: application/json" \
  -d '{
    "branchName": "experiment-v2",
    "baseVersionId": "<VERSION_ID>",
    "author": "tester"
}'
```

---

## 2. Interactive Playground

Verify that you can "play" with a prompt by sending variables and getting a completion.

**Endpoint:** `POST /api/v1/synapse/play`

```bash
curl -X POST http://localhost:8080/api/v1/synapse/play \
  -H "Content-Type: application/json" \
  -d '{
    "promptContent": "Translate {{text}} to {{language}}.",
    "variables": {
        "text": "Hello, world",
        "language": "French"
    },
    "model": "gpt-3.5-turbo"
}'
```

**Expected Output:** A chat completion response (dependent on your LLM provider configuration, or a mock response if in test mode).

---

## 3. Compare V1 vs V2 (Diff)

Verify the semantic diff capability.

**Endpoint:** `POST /api/v1/synapse/diff`

```bash
curl -X POST http://localhost:8080/api/v1/synapse/diff \
  -H "Content-Type: application/json" \
  -d '{
    "original": "You are a helpful assistant.\nAnswer politely.",
    "revised": "You are a helpful expert assistant.\nAnswer precisely."
}'
```

**Expected Output:** A JSON object describing the `deltas` (changes) between the two texts.

---

## 4. Promote to Production

Verify the promotion workflow using the "Deployment" feature.

### A. Deploy/Promote a Version
Promote a specific prompt version to the `production` environment.

**Endpoint:** `POST /api/v1/synapse/deploy`

```bash
curl -X POST http://localhost:8080/api/v1/synapse/deploy \
  -H "Content-Type: application/json" \
  -d '{
    "promptName": "customer-support-bot",
    "versionId": "<VERSION_ID>",
    "environment": "production",
    "user": "release-manager"
}'
```

**Expected Output:** `200 OK`

### B. Verify Promotion Status
Check the workflow status to confirm the version is active in production.

**Endpoint:** `GET /api/v1/synapse/prompts/{promptName}/workflow`

```bash
curl "http://localhost:8080/api/v1/synapse/prompts/customer-support-bot/workflow"
```

**Expected Output:** JSON showing `activeProductionVersionId` matches the one you deployed.
