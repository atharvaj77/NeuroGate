# Testing Configuration Guide

This document outlines the testing strategy and configuration for the NeuroGate core module, focusing on handling external dependencies and integration tests.

## 1. Test Configuration (`application-test.yml`)

We maintain a centralized test configuration file at `core/src/test/resources/application-test.yml`. This file is automatically picked up by Spring Boot tests annotated with `@ActiveProfiles("test")`.

It provides:
- **Dummy API Keys**: For Spring AI auto-configuration (OpenAI, Anthropic, Gemini, Azure, Bedrock) to prevent context load failures.
- **Disabled Services**: Specifically `neurogate.qdrant.enabled=false` to allow running tests without a running Qdrant instance.
- **Resilience Config**: dedicated configuration for `ResilienceIntegrationTest`.

**Do not add sensitive real keys to this file.**

## 2. Integration Tests without External Services

We aim to run the majority of our integration tests without requiring live external services (like OpenAI or Qdrant).

### Strategies:

1.  **Mocking Beans**:
    - Use `@MockBean` (or `@MockitoBean` in newer Spring Boot) to mock service layers that make external calls.
    - Example: Mocking `FaithfulnessJudge` in `ForgeIntegrationTest` to avoid LLM calls.

2.  **Optional Dependencies**:
    - Services that depend on optional infrastructure (like `QdrantClient`) are injected as `Optional<T>` or have `required=false`.
    - Always check `.isPresent()` before using them.

3.  **Dummy Properties**:
    - Spring AI's auto-configuration often validates the presence of API keys at startup. We provide dummy keys (`test-key`) in `application-test.yml` to satisfy these checks.

## 3. Running Tests

### Gradle
Run all core tests:
```bash
./gradlew :core:test
```

Run a specific test class:
```bash
./gradlew :core:test --tests "com.neurogate.forge.ForgeIntegrationTest"
```

### Docker Tests
Some tests (e.g., `NeuroGateIntegrationTest`) may use Testcontainers and require a local Docker environment. If Docker is not available, these tests may fail with `initializationError`. This is expected behavior in non-Docker environments.
