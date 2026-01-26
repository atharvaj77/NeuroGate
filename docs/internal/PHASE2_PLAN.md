# Phase 2: Differentiate - Implementation Plan

**Status:** PLANNED
**Prerequisites:** Phase 1 Complete

---

## Overview

Phase 2 builds on Phase 1's foundation to add differentiating features that no competitor offers:

1. **A/B Testing Framework** - Statistical experimentation for model selection
2. **Semantic Intent Router** - Intelligent model routing based on prompt intent
3. **Streaming Guardrails** - Real-time content moderation with early abort
4. **Structured Output Validation** - JSON schema validation with auto-retry

---

## Existing Infrastructure to Leverage

### For A/B Testing
- `MultiProviderRouter.java:33-67` - Specter Mode (shadow deployment)
- `NeuroGateMetrics.java` - Prometheus metrics collection
- `CostTrackingService.java` - Cost calculation per request

### For Intent Routing
- `ComplexityAnalyzer.java` - Reasoning/coding/creative feature detection
- Weighted scoring: `REASONING_FEATURES`, `CODING_FEATURES`, `CREATIVE_FEATURES`
- `ComplexityScore` with reasoning, domain, creativity, outputLength scores

### For Streaming Guardrails
- `NeuroGuardService.java` - Prompt/output analysis
- `ToxicOutputFilter.java` - Toxic content detection
- `StreamingPiiRestorer.java` - Streaming token processing pattern
- `MultiProviderRouter.routeStream()` - Flux-based streaming

### For Structured Output
- `ChatRequest.java` - Has `responseFormat` field (basic)
- `SentinelService.java` - Main request processing pipeline

---

## Task 1: A/B Testing Framework

### 1.1 Data Models

```
experiment/
├── model/
│   ├── Experiment.java           # Experiment configuration
│   ├── Variant.java              # CONTROL | TREATMENT enum
│   ├── ExperimentResult.java     # Single experiment observation
│   └── ExperimentStats.java      # Aggregated statistics
```

**Experiment.java**
```java
@Data
@Builder
public class Experiment {
    private String experimentId;
    private String name;
    private String description;
    private String controlModel;
    private String treatmentModel;
    private int trafficSplitPercent;  // % going to treatment
    private boolean enabled;
    private Instant startTime;
    private Instant endTime;
    private ExperimentStatus status;  // DRAFT, RUNNING, STOPPED, COMPLETED
}
```

**ExperimentResult.java**
```java
@Data
@Builder
public class ExperimentResult {
    private String experimentId;
    private String requestId;
    private Variant variant;
    private long latencyMs;
    private int inputTokens;
    private int outputTokens;
    private double costUsd;
    private Double qualityScore;  // Optional: from Cortex evaluation
    private Instant timestamp;
}
```

**ExperimentStats.java**
```java
@Data
@Builder
public class ExperimentStats {
    private String experimentId;
    private int controlSamples;
    private int treatmentSamples;

    // Latency stats
    private double controlLatencyMean;
    private double controlLatencyP50;
    private double controlLatencyP99;
    private double treatmentLatencyMean;
    private double treatmentLatencyP50;
    private double treatmentLatencyP99;

    // Cost stats
    private double controlCostMean;
    private double treatmentCostMean;

    // Statistical significance
    private double pValue;
    private double confidenceLevel;
    private boolean statisticallySignificant;
    private String recommendation;  // "CONTROL_BETTER" | "TREATMENT_BETTER" | "NO_DIFFERENCE"
}
```

### 1.2 Services

**ExperimentService.java**
```java
@Service
public class ExperimentService {
    private final Map<String, Experiment> experiments = new ConcurrentHashMap<>();
    private final List<ExperimentResult> results = new CopyOnWriteArrayList<>();

    // Experiment lifecycle
    public Experiment createExperiment(CreateExperimentRequest request);
    public void startExperiment(String experimentId);
    public void stopExperiment(String experimentId);
    public Optional<Experiment> getExperiment(String experimentId);
    public List<Experiment> listExperiments();

    // Variant assignment (deterministic hash for consistency)
    public Variant assignVariant(String experimentId, ChatRequest request);

    // Result recording
    public void recordResult(ExperimentResult result);

    // Statistics
    public ExperimentStats calculateStats(String experimentId);
}
```

**StatisticalCalculator.java**
```java
@Component
public class StatisticalCalculator {
    // Welch's t-test for comparing means
    public TTestResult tTest(double[] control, double[] treatment);

    // Calculate confidence interval
    public ConfidenceInterval confidenceInterval(double[] samples, double confidence);

    // Sample size calculator for desired power
    public int requiredSampleSize(double effectSize, double power, double alpha);
}
```

### 1.3 Controller

**ExperimentController.java**
```java
@RestController
@RequestMapping("/api/v1/experiments")
@Tag(name = "Experiments", description = "A/B testing for model selection")
public class ExperimentController {

    @PostMapping
    @Operation(summary = "Create experiment")
    public Experiment createExperiment(@RequestBody CreateExperimentRequest request);

    @GetMapping
    @Operation(summary = "List all experiments")
    public List<Experiment> listExperiments();

    @GetMapping("/{id}")
    @Operation(summary = "Get experiment details")
    public Experiment getExperiment(@PathVariable String id);

    @PostMapping("/{id}/start")
    @Operation(summary = "Start experiment")
    public Experiment startExperiment(@PathVariable String id);

    @PostMapping("/{id}/stop")
    @Operation(summary = "Stop experiment")
    public Experiment stopExperiment(@PathVariable String id);

    @GetMapping("/{id}/results")
    @Operation(summary = "Get experiment results with statistics")
    public ExperimentStats getResults(@PathVariable String id);
}
```

### 1.4 Integration with MultiProviderRouter

Modify `route()` method:
```java
public ChatResponse route(ChatRequest request) {
    // Check for active experiments
    Optional<Experiment> activeExperiment = experimentService.getActiveExperiment(request);

    if (activeExperiment.isPresent()) {
        Variant variant = experimentService.assignVariant(
            activeExperiment.get().getExperimentId(),
            request
        );

        String modelToUse = variant == Variant.CONTROL
            ? activeExperiment.get().getControlModel()
            : activeExperiment.get().getTreatmentModel();

        request = request.toBuilder().model(modelToUse).build();

        // Record result after completion
        ChatResponse response = routeInternal(request);
        experimentService.recordResult(buildResult(activeExperiment.get(), variant, response));
        return response;
    }

    return routeInternal(request);
}
```

---

## Task 2: Semantic Intent Router

### 2.1 Intent Taxonomy

```java
public enum Intent {
    CODE_GENERATION("Generate new code"),
    CODE_REVIEW("Review or debug existing code"),
    CODE_EXPLANATION("Explain how code works"),
    REASONING("Complex logical reasoning"),
    MATH_SCIENCE("Mathematical or scientific problems"),
    CREATIVE_WRITING("Stories, poems, creative content"),
    SUMMARIZATION("Summarize or condense text"),
    TRANSLATION("Translate between languages"),
    QUESTION_ANSWERING("Direct factual questions"),
    CONVERSATION("General conversation/chat"),
    DATA_ANALYSIS("Analyze data or statistics");

    private final String description;
}
```

### 2.2 Intent Classifier

**IntentClassifier.java**
```java
@Service
@RequiredArgsConstructor
public class IntentClassifier {

    private final ComplexityAnalyzer complexityAnalyzer;

    // Intent detection patterns
    private static final Map<Intent, List<Pattern>> INTENT_PATTERNS = Map.of(
        Intent.CODE_GENERATION, List.of(
            Pattern.compile("(?i)write\\s+(a\\s+)?(function|class|method|code|script)"),
            Pattern.compile("(?i)create\\s+(a\\s+)?(program|api|endpoint)"),
            Pattern.compile("(?i)implement\\s+")
        ),
        Intent.CODE_REVIEW, List.of(
            Pattern.compile("(?i)(fix|debug|review|refactor)\\s+(this|the|my)"),
            Pattern.compile("(?i)what'?s\\s+wrong\\s+with"),
            Pattern.compile("(?i)why\\s+(is|does)\\s+(this|it)\\s+(not\\s+work|fail)")
        ),
        // ... more patterns
    );

    public IntentClassification classify(String prompt) {
        ComplexityScore complexity = complexityAnalyzer.analyze(prompt);

        // Pattern-based detection
        Map<Intent, Double> patternScores = calculatePatternScores(prompt);

        // Complexity-based adjustment
        adjustForComplexity(patternScores, complexity);

        // Get top intent
        Intent topIntent = patternScores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(Intent.CONVERSATION);

        return IntentClassification.builder()
            .intent(topIntent)
            .confidence(patternScores.get(topIntent))
            .allScores(patternScores)
            .complexityScore(complexity)
            .build();
    }
}
```

### 2.3 Intent-to-Model Mapping

**IntentRoutingConfig.java** (configurable via application.yml)
```java
@ConfigurationProperties(prefix = "neurogate.intent-routing")
@Data
public class IntentRoutingConfig {
    private boolean enabled = false;
    private Map<Intent, List<ModelRecommendation>> mappings;
}
```

**application.yml**
```yaml
neurogate:
  intent-routing:
    enabled: true
    mappings:
      CODE_GENERATION:
        - model: gpt-4o
          priority: 1
          reason: "Excellent code generation with function calling"
        - model: claude-3-5-sonnet-20241022
          priority: 2
          reason: "Strong reasoning for complex code"
        - model: codestral
          priority: 3
          reason: "Specialized for code tasks"
      REASONING:
        - model: o1-preview
          priority: 1
          reason: "Dedicated reasoning model"
        - model: claude-3-5-sonnet-20241022
          priority: 2
          reason: "Strong analytical capabilities"
      CREATIVE_WRITING:
        - model: claude-3-opus-20240229
          priority: 1
          reason: "Best creative output quality"
        - model: gpt-4o
          priority: 2
          reason: "Versatile creative capabilities"
      SUMMARIZATION:
        - model: gpt-4o-mini
          priority: 1
          reason: "Fast and cost-effective"
        - model: gemini-1.5-flash
          priority: 2
          reason: "Large context window"
```

### 2.4 IntentRouter Service

**IntentRouter.java**
```java
@Service
@RequiredArgsConstructor
public class IntentRouter {

    private final IntentClassifier classifier;
    private final IntentRoutingConfig config;

    public RoutingDecision route(ChatRequest request) {
        if (!config.isEnabled()) {
            return RoutingDecision.passthrough(request);
        }

        // Check for intent override header
        String intentOverride = request.getIntentOverride();
        if (intentOverride != null) {
            Intent intent = Intent.valueOf(intentOverride);
            return routeToIntent(request, intent, 1.0);
        }

        // Classify intent
        IntentClassification classification = classifier.classify(
            extractPromptText(request)
        );

        // Only route if confidence is high enough
        if (classification.getConfidence() < 0.6) {
            return RoutingDecision.passthrough(request);
        }

        return routeToIntent(request, classification.getIntent(), classification.getConfidence());
    }

    private RoutingDecision routeToIntent(ChatRequest request, Intent intent, double confidence) {
        List<ModelRecommendation> recommendations = config.getMappings().get(intent);

        if (recommendations == null || recommendations.isEmpty()) {
            return RoutingDecision.passthrough(request);
        }

        // Get first available model
        ModelRecommendation selected = recommendations.get(0);

        return RoutingDecision.builder()
            .originalRequest(request)
            .modifiedModel(selected.getModel())
            .intent(intent)
            .confidence(confidence)
            .reason(selected.getReason())
            .build();
    }
}
```

---

## Task 3: Streaming Guardrails

### 3.1 Core Components

**StreamingGuardrail.java**
```java
@Component
public class StreamingGuardrail {

    private final StreamingGuardrailConfig config;
    private final List<ToxicityPattern> patterns;

    // Thread-local state for each stream
    private static final ThreadLocal<GuardrailState> STATE = ThreadLocal.withInitial(GuardrailState::new);

    @Data
    private static class GuardrailState {
        private StringBuilder buffer = new StringBuilder();
        private int toxicityScore = 0;
        private int warningCount = 0;
        private boolean aborted = false;
    }

    public StreamingResult processToken(String token) {
        GuardrailState state = STATE.get();

        if (state.isAborted()) {
            return StreamingResult.aborted("Stream previously aborted");
        }

        // Add to buffer
        state.getBuffer().append(token);

        // Trim buffer to max size
        if (state.getBuffer().length() > config.getBufferSize()) {
            state.getBuffer().delete(0, state.getBuffer().length() - config.getBufferSize());
        }

        // Check patterns
        String bufferContent = state.getBuffer().toString();
        for (ToxicityPattern pattern : patterns) {
            if (pattern.getPattern().matcher(bufferContent).find()) {
                if (pattern.getSeverity() == Severity.HIGH) {
                    state.setAborted(true);
                    return StreamingResult.abort(
                        "Content policy violation detected",
                        pattern.getCategory()
                    );
                } else if (pattern.getSeverity() == Severity.MEDIUM) {
                    state.setWarningCount(state.getWarningCount() + 1);
                    state.setToxicityScore(state.getToxicityScore() + 20);
                }
            }
        }

        // Check toxicity threshold
        if (state.getToxicityScore() > config.getToxicityThreshold()) {
            state.setAborted(true);
            return StreamingResult.abort(
                "Cumulative toxicity threshold exceeded",
                "TOXICITY_THRESHOLD"
            );
        }

        return StreamingResult.ok(token, state.getToxicityScore());
    }

    public void reset() {
        STATE.remove();
    }
}
```

**StreamingResult.java**
```java
@Data
@Builder
public class StreamingResult {
    private String token;
    private boolean shouldContinue;
    private String abortReason;
    private String abortCategory;
    private int toxicityLevel;

    public static StreamingResult ok(String token, int toxicity) {
        return StreamingResult.builder()
            .token(token)
            .shouldContinue(true)
            .toxicityLevel(toxicity)
            .build();
    }

    public static StreamingResult abort(String reason, String category) {
        return StreamingResult.builder()
            .shouldContinue(false)
            .abortReason(reason)
            .abortCategory(category)
            .build();
    }
}
```

### 3.2 Configuration

**StreamingGuardrailConfig.java**
```java
@ConfigurationProperties(prefix = "neurogate.streaming-guardrails")
@Data
public class StreamingGuardrailConfig {
    private boolean enabled = true;
    private int toxicityThreshold = 70;
    private int bufferSize = 500;
    private List<PatternConfig> patterns;

    @Data
    public static class PatternConfig {
        private String pattern;
        private Severity severity;
        private String category;
    }
}
```

### 3.3 Integration with routeStream()

```java
public Flux<ChatResponse> routeStream(ChatRequest request) {
    // ... existing routing logic ...

    return resultFlux
        .map(response -> {
            if (!streamingGuardrailConfig.isEnabled()) {
                return response;
            }

            String content = extractContent(response);
            if (content == null) return response;

            StreamingResult result = streamingGuardrail.processToken(content);

            if (!result.isShouldContinue()) {
                // Transform to abort message
                response.getChoices().get(0).getDelta().setContent(
                    "\n\n[Stream terminated: " + result.getAbortReason() + "]"
                );
                response.getChoices().get(0).setFinishReason("content_filter");
            }

            return response;
        })
        .takeUntil(response -> {
            String finishReason = response.getChoices().get(0).getFinishReason();
            return "content_filter".equals(finishReason);
        })
        .doFinally(signal -> streamingGuardrail.reset());
}
```

---

## Task 4: Structured Output Validation

### 4.1 Schema Model

**JsonSchemaRequest.java**
```java
@Data
@Builder
public class JsonSchemaRequest {
    private String type;  // "json_object" or "json_schema"
    private JsonSchemaDefinition jsonSchema;

    @Data
    @Builder
    public static class JsonSchemaDefinition {
        private String name;
        private boolean strict;
        private Map<String, Object> schema;  // JSON Schema object
    }
}
```

### 4.2 Validator

**StructuredOutputValidator.java**
```java
@Service
public class StructuredOutputValidator {

    private final ObjectMapper objectMapper;
    private final JsonSchemaFactory schemaFactory;

    public ValidationResult validate(String response, JsonSchemaRequest schemaRequest) {
        try {
            // Parse response as JSON
            JsonNode responseNode = objectMapper.readTree(response);

            // Build JSON Schema
            JsonSchema schema = schemaFactory.getSchema(
                objectMapper.valueToTree(schemaRequest.getJsonSchema().getSchema())
            );

            // Validate
            Set<ValidationMessage> errors = schema.validate(responseNode);

            if (errors.isEmpty()) {
                return ValidationResult.valid(response);
            }

            return ValidationResult.invalid(
                response,
                errors.stream()
                    .map(e -> new ValidationError(e.getPath(), e.getMessage()))
                    .toList()
            );

        } catch (JsonProcessingException e) {
            return ValidationResult.invalid(
                response,
                List.of(new ValidationError("$", "Invalid JSON: " + e.getMessage()))
            );
        }
    }

    public String generateCorrectionHint(ValidationResult result) {
        StringBuilder hint = new StringBuilder();
        hint.append("The response did not match the expected JSON schema. Errors:\n");

        for (ValidationError error : result.getErrors()) {
            hint.append("- At path '").append(error.getPath()).append("': ")
                .append(error.getMessage()).append("\n");
        }

        hint.append("\nPlease provide a valid JSON response matching the schema.");
        return hint.toString();
    }
}
```

### 4.3 Retry Service

**StructuredOutputService.java**
```java
@Service
@RequiredArgsConstructor
public class StructuredOutputService {

    private final StructuredOutputValidator validator;
    private final MultiProviderRouter router;
    private final int maxRetries = 3;

    public ChatResponse generateWithValidation(ChatRequest request) {
        JsonSchemaRequest schemaRequest = request.getResponseFormat();

        if (schemaRequest == null || !"json_schema".equals(schemaRequest.getType())) {
            return router.route(request);
        }

        int retries = 0;
        ChatResponse lastResponse = null;
        List<ValidationError> lastErrors = null;

        while (retries < maxRetries) {
            ChatRequest currentRequest = retries == 0
                ? request
                : buildRetryRequest(request, lastResponse, lastErrors);

            lastResponse = router.route(currentRequest);
            String content = extractContent(lastResponse);

            ValidationResult result = validator.validate(content, schemaRequest);

            if (result.isValid()) {
                lastResponse.setValidation(ValidationMetadata.builder()
                    .schemaValid(true)
                    .retriesNeeded(retries)
                    .build());
                return lastResponse;
            }

            lastErrors = result.getErrors();
            retries++;
        }

        // Max retries exceeded
        lastResponse.setValidation(ValidationMetadata.builder()
            .schemaValid(false)
            .retriesNeeded(retries)
            .errors(lastErrors)
            .build());

        return lastResponse;
    }

    private ChatRequest buildRetryRequest(
        ChatRequest original,
        ChatResponse invalidResponse,
        List<ValidationError> errors
    ) {
        String hint = validator.generateCorrectionHint(
            ValidationResult.invalid(extractContent(invalidResponse), errors)
        );

        List<Message> messages = new ArrayList<>(original.getMessages());
        messages.add(Message.builder()
            .role("assistant")
            .content(extractContent(invalidResponse))
            .build());
        messages.add(Message.builder()
            .role("user")
            .content(hint)
            .build());

        return original.toBuilder()
            .messages(messages)
            .build();
    }
}
```

---

## Files Summary

### New Files (19)

| Package | File | Purpose |
|---------|------|---------|
| `experiment.model` | `Experiment.java` | Experiment configuration |
| `experiment.model` | `ExperimentResult.java` | Single observation |
| `experiment.model` | `ExperimentStats.java` | Aggregated statistics |
| `experiment.model` | `Variant.java` | CONTROL/TREATMENT enum |
| `experiment` | `ExperimentService.java` | Experiment management |
| `experiment` | `ExperimentController.java` | REST API |
| `experiment` | `StatisticalCalculator.java` | T-test, CI calculations |
| `router.intelligence` | `IntentClassifier.java` | Classify prompt intent |
| `router.intelligence` | `IntentRouter.java` | Route by intent |
| `router.intelligence.model` | `Intent.java` | Intent enum |
| `router.intelligence.model` | `IntentClassification.java` | Classification result |
| `router.intelligence.model` | `RoutingDecision.java` | Routing decision |
| `vault.streaming` | `StreamingGuardrail.java` | Token-by-token guard |
| `vault.streaming` | `StreamingResult.java` | Guard result |
| `vault.streaming` | `StreamingGuardrailConfig.java` | Configuration |
| `validation` | `StructuredOutputValidator.java` | JSON schema validator |
| `validation` | `StructuredOutputService.java` | Validation with retry |
| `validation.model` | `ValidationResult.java` | Validation result |
| `validation.model` | `ValidationError.java` | Single error |

### Modified Files (7)

| File | Changes |
|------|---------|
| `router/provider/MultiProviderRouter.java` | Integrate experiments, intent routing, streaming guardrails |
| `router/intelligence/ComplexityAnalyzer.java` | Expose scores for intent classification |
| `sentinel/model/ChatRequest.java` | Add `responseFormat` with JSON schema support |
| `sentinel/model/ChatResponse.java` | Add `validation` metadata |
| `sentinel/SentinelService.java` | Integrate structured output validation |
| `application.yml` | Add intent-routing and streaming-guardrails config |
| `build.gradle.kts` | Add json-schema-validator dependency |

---

## Dependencies to Add

```kotlin
// JSON Schema Validation
implementation("com.networknt:json-schema-validator:1.0.87")

// Apache Commons Math for statistics
implementation("org.apache.commons:commons-math3:3.6.1")
```

---

## Testing Strategy

### A/B Testing
```bash
# Create experiment
curl -X POST http://localhost:8080/api/v1/experiments \
  -H "Content-Type: application/json" \
  -d '{
    "name": "GPT-4o vs Claude Sonnet",
    "controlModel": "gpt-4o",
    "treatmentModel": "claude-3-5-sonnet-20241022",
    "trafficSplitPercent": 50
  }'

# Start experiment
curl -X POST http://localhost:8080/api/v1/experiments/{id}/start

# Make requests (will be automatically assigned to variants)
# ...

# Check results
curl http://localhost:8080/api/v1/experiments/{id}/results
```

### Intent Routing
```bash
# Code generation (should route to gpt-4o)
curl -X POST http://localhost:8080/v1/chat/completions \
  -d '{"messages":[{"role":"user","content":"Write a Python function to merge two sorted lists"}]}'

# Reasoning (should route to o1-preview)
curl -X POST http://localhost:8080/v1/chat/completions \
  -d '{"messages":[{"role":"user","content":"Prove that the square root of 2 is irrational step by step"}]}'

# With override
curl -X POST http://localhost:8080/v1/chat/completions \
  -H "X-Intent-Override: CREATIVE_WRITING" \
  -d '{"messages":[...]}'
```

### Streaming Guardrails
```bash
# Normal stream
curl -N http://localhost:8080/v1/chat/completions \
  -d '{"stream":true,"messages":[{"role":"user","content":"Tell me a story"}]}'

# Should trigger guard (test with adversarial prompt)
# Response will end with content_filter finish_reason if triggered
```

### Structured Output
```bash
curl -X POST http://localhost:8080/v1/chat/completions \
  -d '{
    "model": "gpt-4o",
    "messages": [{"role":"user","content":"Generate a user profile for John Doe"}],
    "response_format": {
      "type": "json_schema",
      "json_schema": {
        "name": "user_profile",
        "strict": true,
        "schema": {
          "type": "object",
          "properties": {
            "name": {"type": "string"},
            "age": {"type": "integer"},
            "email": {"type": "string", "format": "email"}
          },
          "required": ["name", "age", "email"]
        }
      }
    }
  }'
```

---

## Resume Impact

After completing Phase 2:

1. **A/B Testing Framework**
   > "Built statistical A/B testing platform for LLM model selection with Welch's t-test significance calculation"

2. **Semantic Intent Router**
   > "Implemented ML-based intent classification for intelligent model routing, reducing costs by routing simple tasks to efficient models"

3. **Streaming Guardrails**
   > "Designed real-time streaming content moderation with early-abort capability for token-by-token safety filtering"

4. **Structured Output Validation**
   > "Created JSON schema validation layer with auto-retry for reliable structured LLM outputs"
