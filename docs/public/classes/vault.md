# Vault Module Documentation

The **Vault** module is responsible for privacy, security, and data protection within the NeuroGate system. It handles PII sanitization, splitting sensitive data from requests before they are sent to LLM providers, and restoring it in responses. It also includes **NeuroGuard**, an active defense system for detecting jailbreaks and prompt injections.

## Core Services

### `PiiSanitizationService`
**Package:** `com.neurogate.vault`

**Purpose:**
The primary service for detecting and replacing Personally Identifiable Information (PII) in text and multimodal content. It orchestrates various `PiiDetector` implementations to find sensitive data and uses `TokenVault` to replace them with reversible tokens (e.g., `<EMAIL_1>`).

**Key Methods:**
- `sanitize(String text)`: Scans text for PII, tokenizes matches, and returns a `SanitizedPrompt` containing the safe text and a map of tokens.
- `sanitizeContent(Object content)`: Handles multimodal content (String or List of Maps), recursively sanitizing text fields and handling image URLs (redacting images with PII).
- `desanitize(String sanitizedText)`: Restores original PII values by replacing tokens with their original values.
- `containsPii(String text)`: Boolean check for PII presence.
- `getStats()`: Returns statistics on detected and tokenized PII.

**Context:**
Used by the `NeuralRouter` (or equivalent gateway component) to sanitize user prompts before forwarding them to external model providers.

---

### `StreamingPiiRestorer`
**Package:** `com.neurogate.vault`

**Purpose:**
Handles PII restoration for streaming responses (SSE). Since tokens might be split across multiple chunks (e.g., `<EM` + `AIL_1>`), this class uses a sliding window buffer to detect and restore complete tokens in real-time without breaking the stream.

**Key Methods:**
- `processChunk(String chunk)`: Adds chunk to buffer, checks for complete tokens, and returns restored content or buffers partial tokens.
- `flush()`: Returns any remaining content in the buffer when the stream ends.
- `reset()`: Clears the buffer for a new request.

**Context:**
Instantiated per-request via `PiiRestorerFactory` and used by the streaming response handler to ensure users see their original data while the underlying provider only sees tokens.

---

### `PiiRestorerFactory`
**Package:** `com.neurogate.vault`

**Purpose:**
Factory for creating `StreamingPiiRestorer` instances. Since `StreamingPiiRestorer` is stateful (contains a buffer), a new instance is required for each streaming request.

**Key Methods:**
- `createRestorer()`: Returns a new `StreamingPiiRestorer` injected with the `PiiSanitizationService`.

---

## Detectors

### `PiiDetector` (Interface)
**Package:** `com.neurogate.vault.detector`

**Purpose:**
Defines the contract for PII detection strategies.

**Key Methods:**
- `detect(String text)`: Returns a list of `PiiEntity` objects found in the text.
- `getName()`: Returns the name of the detector implementation.

### `RegexPiiDetector`
**Package:** `com.neurogate.vault.detector`

**Purpose:**
Implementation of `PiiDetector` that uses Regular Expressions to identify common PII types.

**Supported Types:**
- **Email**: Standard email patterns.
- **SSN**: US Social Security Numbers (with basic validation).
- **Phone**: US phone number formats.
- **Credit Card**: 16-digit numbers (validated with Luhn algorithm).
- **IP Address**: IPv4 addresses.

**Context:**
One of the detectors in the `PiiSanitizationService` chain. Fast and effective for structured PII.

---

## NeuroGuard

### `NeuroGuardController`
**Package:** `com.neurogate.vault.neuroguard`

**Purpose:**
REST API controller for the NeuroGuard security features. Exposes endpoints for analyzing prompts and outputs for threats like jailbreaks, toxicity, and prompt injection.

**Key Methods:**
- `analyzePrompt(AnalyzeRequest)`: Scans a user prompt for attacks.
- `analyzeOutput(AnalyzeRequest)`: Scans model output for harmful content.
- `fullScan(ScanRequest)`: Performs a comprehensive security scan.
- `getStatistics()`: Returns metrics on blocked threats.

**Context:**
The external interface for the active defense system, likely used by admin dashboards or as a sidecar check in the gateway pipeline.
