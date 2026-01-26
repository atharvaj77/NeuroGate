package com.neurogate.router.provider;

import com.neurogate.exception.AllProvidersFailedException;
import com.neurogate.experiment.ExperimentService;
import com.neurogate.experiment.model.Experiment;
import com.neurogate.experiment.model.ExperimentResult;
import com.neurogate.experiment.model.Variant;
import com.neurogate.metrics.NeuroGateMetrics;
import com.neurogate.router.resilience.ResilienceService;
import com.neurogate.router.shadow.ShadowDeploymentService;
import com.neurogate.sentinel.model.ChatRequest;
import com.neurogate.sentinel.model.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Intelligently routes chat requests to the optimal LLM provider.
 *
 * <p>The MultiProviderRouter is the central routing component of NeuroGate,
 * responsible for directing requests to appropriate LLM providers based on:</p>
 * <ul>
 *   <li>Model availability and health status</li>
 *   <li>Intent-based routing (matching task type to optimal model)</li>
 *   <li>A/B testing experiments</li>
 *   <li>Shadow deployments for comparison testing</li>
 *   <li>Fallback chains when primary providers fail</li>
 * </ul>
 *
 * <h2>Routing Flow</h2>
 * <pre>
 * Request → Shadow Mode Check → Intent Routing → A/B Test → Provider Selection → Response
 *                                                              ↓ (on failure)
 *                                                         Fallback Chain
 * </pre>
 *
 * <h2>Streaming Support</h2>
 * <p>The router supports both synchronous ({@link #route}) and streaming
 * ({@link #routeStream}) request handling. Streaming responses are processed
 * through a pipeline that includes:</p>
 * <ul>
 *   <li>PII token restoration (reversing tokenization from input sanitization)</li>
 *   <li>Content safety guardrails (detecting harmful content in real-time)</li>
 * </ul>
 *
 * <h2>Configuration</h2>
 * <p>The router auto-discovers all {@link LLMProvider} beans and optional services:</p>
 * <ul>
 *   <li>{@code ExperimentService} - Enables A/B testing</li>
 *   <li>{@code IntentRouter} - Enables intent-based model selection</li>
 *   <li>{@code StreamingGuardrail} - Enables real-time content filtering</li>
 *   <li>{@code ShadowDeploymentService} - Enables shadow request execution</li>
 * </ul>
 *
 * @see LLMProvider
 * @see com.neurogate.router.strategy.RoutingStrategy
 * @see com.neurogate.experiment.ExperimentService
 */
@Slf4j
@Service
public class MultiProviderRouter {

    private final List<LLMProvider> providers;
    private final NeuroGateMetrics metrics;
    private final ResilienceService resilienceService;
    private final com.neurogate.vault.PiiSanitizationService piiSanitizationService;

    // Optional: A/B testing integration
    private ExperimentService experimentService;

    // Optional: Intent-based routing
    private com.neurogate.router.intelligence.IntentRouter intentRouter;

    // Optional: Streaming guardrails
    private com.neurogate.vault.streaming.StreamingGuardrail streamingGuardrail;

    // Shadow deployment service
    private ShadowDeploymentService shadowDeploymentService;

    @Autowired
    public MultiProviderRouter(
            List<LLMProvider> providers,
            NeuroGateMetrics metrics,
            ResilienceService resilienceService,
            com.neurogate.vault.PiiSanitizationService piiSanitizationService
    ) {
        this.providers = providers;
        this.metrics = metrics;
        this.resilienceService = resilienceService;
        this.piiSanitizationService = piiSanitizationService;
    }

    @Autowired(required = false)
    public void setShadowDeploymentService(ShadowDeploymentService shadowDeploymentService) {
        this.shadowDeploymentService = shadowDeploymentService;
        log.info("Shadow deployment service enabled");
    }

    @Autowired(required = false)
    public void setExperimentService(ExperimentService experimentService) {
        this.experimentService = experimentService;
        log.info("🧪 A/B Testing integration enabled");
    }

    @Autowired(required = false)
    public void setIntentRouter(com.neurogate.router.intelligence.IntentRouter intentRouter) {
        this.intentRouter = intentRouter;
        log.info("🎯 Intent-based routing integration enabled");
    }

    @Autowired(required = false)
    public void setStreamingGuardrail(com.neurogate.vault.streaming.StreamingGuardrail streamingGuardrail) {
        this.streamingGuardrail = streamingGuardrail;
        log.info("🛡️ Streaming guardrails integration enabled");
    }

    public ChatResponse route(ChatRequest request) {
        String requestedModel = request.getModel();
        log.debug("Multi-provider routing request for model: {}", requestedModel);

        // --- SPECTER MODE (Shadow Deployment) ---
        if (shadowDeploymentService != null && request.getShadowModel() != null && !request.getShadowModel().isEmpty()) {
            shadowDeploymentService.executeShadowRequest(request, request.getShadowModel(), this::route);
        }
        // -----------------------------------------

        // --- INTENT-BASED ROUTING ---
        if (intentRouter != null && intentRouter.isEnabled()) {
            com.neurogate.router.intelligence.model.RoutingDecision routingDecision =
                    intentRouter.route(request);

            if (routingDecision.isIntentRoutingApplied()) {
                log.info("🎯 Intent routing applied: {} → {} (intent: {}, confidence: {:.2f})",
                        request.getModel(),
                        routingDecision.getSelectedModel(),
                        routingDecision.getIntent(),
                        routingDecision.getConfidence());

                requestedModel = routingDecision.getSelectedModel();
                request = request.toBuilder().model(requestedModel).build();
            }
        }
        // -----------------------------------------

        // --- A/B TESTING (Experiment Mode) ---
        ExperimentContext experimentContext = null;
        if (experimentService != null) {
            Optional<Experiment> activeExperiment = experimentService.findActiveExperiment(request);

            if (activeExperiment.isPresent()) {
                Experiment experiment = activeExperiment.get();
                Variant variant = experimentService.assignVariant(experiment.getExperimentId(), request);
                String experimentModel = experimentService.getModelForVariant(
                        experiment.getExperimentId(), variant);

                log.info("🧪 A/B Test: Experiment '{}' assigned {} → model '{}'",
                        experiment.getName(), variant, experimentModel);

                // Override the requested model
                requestedModel = experimentModel;
                request = request.toBuilder().model(experimentModel).build();

                // Save context for result recording
                experimentContext = new ExperimentContext(
                        experiment.getExperimentId(),
                        variant,
                        experimentModel,
                        System.currentTimeMillis()
                );
            }
        }
        // -----------------------------------------

        Optional<LLMProvider> directProvider = findProviderForModel(requestedModel);

        if (directProvider.isPresent() && directProvider.get().isAvailable()) {
            LLMProvider provider = directProvider.get();
            log.info("Routing to direct provider: {} for model: {}", provider.getName(), requestedModel);
            metrics.recordProviderRequest(provider.getName());

            // Execute with circuit breaker and retry, fallback to other providers
            final ExperimentContext finalExpContext = experimentContext;
            final ChatRequest finalRequest = request;
            ChatResponse response = resilienceService.execute(
                    provider.getName(),
                    () -> provider.generate(finalRequest),
                    throwable -> {
                        log.warn("Direct provider {} failed with resilience: {}", provider.getName(),
                                throwable.getMessage());
                        metrics.recordProviderFailure(provider.getName());
                        return routeToFallbackProviders(finalRequest, finalRequest.getModel(), provider.getName());
                    });

            // Record experiment result if in A/B test
            recordExperimentResult(finalExpContext, response, true, null);
            return response;
        }

        // No direct provider, go straight to fallbacks
        ChatResponse response = routeToFallbackProviders(request, requestedModel, null);
        recordExperimentResult(experimentContext, response, true, null);
        return response;
    }

    /**
     * Record experiment result for A/B testing.
     */
    private void recordExperimentResult(
            ExperimentContext context,
            ChatResponse response,
            boolean success,
            String errorMessage
    ) {
        if (context == null || experimentService == null) return;

        long latencyMs = System.currentTimeMillis() - context.startTimeMs();

        ExperimentResult result = ExperimentResult.builder()
                .experimentId(context.experimentId())
                .requestId(response.getId() != null ? response.getId() : UUID.randomUUID().toString())
                .variant(context.variant())
                .modelUsed(context.model())
                .latencyMs(latencyMs)
                .inputTokens(response.getUsage() != null ? response.getUsage().getPromptTokens() : 0)
                .outputTokens(response.getUsage() != null ? response.getUsage().getCompletionTokens() : 0)
                .costUsd(response.getCostUsd() != null ? response.getCostUsd() : 0.0)
                .success(success)
                .errorMessage(errorMessage)
                .build();

        experimentService.recordResult(result);
    }

    /**
     * Context for tracking A/B test experiments.
     */
    private record ExperimentContext(
            String experimentId,
            Variant variant,
            String model,
            long startTimeMs
    ) {}

    /**
     * Route to fallback providers when primary fails
     */
    private ChatResponse routeToFallbackProviders(ChatRequest request, String requestedModel, String excludeProvider) {
        List<LLMProvider> fallbackProviders = getAvailableProvidersByPriority();

        for (LLMProvider provider : fallbackProviders) {
            if (!provider.isAvailable() || provider.getName().equals(excludeProvider)) {
                continue;
            }

            String equivalentModel = provider.getEquivalentModel(requestedModel);
            log.info("Routing to fallback provider: {} with equivalent model: {} (original: {})",
                    provider.getName(), equivalentModel, requestedModel);

            ChatRequest fallbackRequest = ChatRequest.builder()
                    .model(equivalentModel)
                    .messages(request.getMessages())
                    .temperature(request.getTemperature())
                    .maxTokens(request.getMaxTokens())
                    .topP(request.getTopP())
                    .frequencyPenalty(request.getFrequencyPenalty())
                    .presencePenalty(request.getPresencePenalty())
                    .stop(request.getStop())
                    .stream(request.getStream())
                    .user(request.getUser())
                    .build();

            try {
                metrics.recordProviderRequest(provider.getName());
                ChatResponse response = resilienceService.execute(
                        provider.getName(),
                        () -> provider.generate(fallbackRequest),
                        null // No further fallback within this level
                );
                response.setRoute(provider.getName() + "-fallback");
                return response;
            } catch (Exception e) {
                log.warn("Fallback provider {} failed: {}", provider.getName(), e.getMessage());
                metrics.recordProviderFailure(provider.getName());
            }
        }

        List<String> attemptedProviders = fallbackProviders.stream()
                .map(LLMProvider::getName)
                .toList();
        throw new AllProvidersFailedException(attemptedProviders);
    }

    public Flux<ChatResponse> routeStream(ChatRequest request) {
        String requestedModel = request.getModel();
        log.debug("Multi-provider routing streaming request for model: {}", requestedModel);

        Flux<ChatResponse> resultFlux = Flux.empty();

        // Try to find a provider that directly supports the requested model
        Optional<LLMProvider> directProvider = findProviderForModel(requestedModel);

        if (directProvider.isPresent() && directProvider.get().isAvailable()) {
            try {
                log.info("Routing streaming to direct provider: {} for model: {}",
                        directProvider.get().getName(), requestedModel);
                resultFlux = directProvider.get().generateStream(request);
            } catch (Exception e) {
                log.warn("Direct provider {} streaming failed, trying fallback: {}",
                        directProvider.get().getName(), e.getMessage());
                // Fallthrough to fallback logic
                resultFlux = routeStreamFallback(request, requestedModel);
            }
        } else {
            resultFlux = routeStreamFallback(request, requestedModel);
        }

        // --- STREAMING PII REDACTION ---
        com.neurogate.vault.StreamingPiiRestorer piiRestorer = new com.neurogate.vault.StreamingPiiRestorer(
                piiSanitizationService);

        // We use 'handle' to maintain state references if needed, but 'map' is fine if
        // we append a flush.
        // To properly flush, we need to construct a valid ChatResponse. We can capture
        // metadata from the first packet.
        java.util.concurrent.atomic.AtomicReference<ChatResponse> lastResponseRef = new java.util.concurrent.atomic.AtomicReference<>();

        // Reset streaming guardrail for new stream
        if (streamingGuardrail != null) {
            streamingGuardrail.reset();
        }

        // Track if stream was aborted by guardrail
        java.util.concurrent.atomic.AtomicBoolean guardrailAborted = new java.util.concurrent.atomic.AtomicBoolean(false);

        return resultFlux
                .map(chatResponse -> {
                    lastResponseRef.set(chatResponse); // Capture metadata
                    if (chatResponse.getChoices() != null && !chatResponse.getChoices().isEmpty()) {
                        String content = chatResponse.getChoices().get(0).getDelta().getStrContent();
                        if (content != null) {
                            // --- PII Redaction ---
                            String restored = piiRestorer.processChunk(content);
                            if (!restored.equals(content) && !restored.isEmpty()) {
                                chatResponse.setPiiDetected(1);
                            }

                            // --- Streaming Guardrail ---
                            if (streamingGuardrail != null && streamingGuardrail.isEnabled()) {
                                com.neurogate.vault.streaming.StreamingResult guardResult =
                                        streamingGuardrail.processToken(restored);

                                if (!guardResult.isShouldContinue()) {
                                    // Mark as aborted and modify response
                                    guardrailAborted.set(true);
                                    chatResponse.getChoices().get(0).getDelta().setContent(
                                            "\n\n[Stream terminated: " + guardResult.getAbortReason() + "]");
                                    chatResponse.getChoices().get(0).setFinishReason("content_filter");
                                    log.warn("🛡️ Stream guardrail triggered: {}",
                                            guardResult.getViolationCategory());
                                } else {
                                    // Use potentially modified token
                                    chatResponse.getChoices().get(0).getDelta().setContent(
                                            guardResult.getToken() != null ? guardResult.getToken() : restored);
                                }
                            } else {
                                chatResponse.getChoices().get(0).getDelta().setContent(restored);
                            }
                        }
                    }
                    return chatResponse;
                })
                // Stop stream early if guardrail aborted
                .takeUntil(response -> {
                    if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                        String finishReason = response.getChoices().get(0).getFinishReason();
                        return "content_filter".equals(finishReason);
                    }
                    return false;
                })
                .concatWith(Flux.defer(() -> {
                    // Skip flush if guardrail aborted
                    if (guardrailAborted.get()) {
                        return Flux.empty();
                    }

                    String remainder = piiRestorer.flush();
                    if (remainder == null || remainder.isEmpty()) {
                        return Flux.empty();
                    }

                    // Create a flush packet using metadata from the last seen response
                    ChatResponse template = lastResponseRef.get();
                    if (template == null)
                        return Flux.empty(); // Should not happen if stream emitted something

                    ChatResponse flushPacket = ChatResponse.builder()
                            .id(template.getId())
                            .object(template.getObject())
                            .created(template.getCreated())
                            .model(template.getModel())
                            .traceId(template.getTraceId())
                            .sessionId(template.getSessionId())
                            .choices(java.util.List.of(com.neurogate.sentinel.model.Choice.builder()
                                    .index(0)
                                    .delta(com.neurogate.sentinel.model.Message.builder()
                                            .role("assistant") // Delta role
                                            .content(remainder)
                                            .build())
                                    .build()))
                            .build();
                    return Flux.just(flushPacket);
                }))
                .doFinally(signal -> {
                    // Clean up guardrail state
                    if (streamingGuardrail != null) {
                        streamingGuardrail.reset();
                    }
                });
    }

    private Flux<ChatResponse> routeStreamFallback(ChatRequest request, String requestedModel) {
        // Try fallback providers
        List<LLMProvider> fallbackProviders = getAvailableProvidersByPriority();

        for (LLMProvider provider : fallbackProviders) {
            if (!provider.isAvailable()) {
                continue;
            }

            try {
                String equivalentModel = provider.getEquivalentModel(requestedModel);
                log.info("Routing streaming to fallback provider: {} with equivalent model: {}",
                        provider.getName(), equivalentModel);

                ChatRequest fallbackRequest = ChatRequest.builder()
                        .model(equivalentModel)
                        .messages(request.getMessages())
                        .temperature(request.getTemperature())
                        .maxTokens(request.getMaxTokens())
                        .topP(request.getTopP())
                        .frequencyPenalty(request.getFrequencyPenalty())
                        .presencePenalty(request.getPresencePenalty())
                        .stop(request.getStop())
                        .stream(request.getStream())
                        .user(request.getUser())
                        .build();

                return provider.generateStream(fallbackRequest);

            } catch (Exception e) {
                log.warn("Fallback provider {} streaming failed: {}", provider.getName(), e.getMessage());
                // Continue to next fallback
            }
        }
        List<String> attemptedProviders = fallbackProviders.stream()
                .map(LLMProvider::getName)
                .toList();
        return Flux.error(new AllProvidersFailedException("All LLM providers failed for streaming", attemptedProviders));
    }

    /**
     * Find provider that directly supports the requested model
     */
    private Optional<LLMProvider> findProviderForModel(String model) {
        return providers.stream()
                .filter(p -> p.supportsModel(model))
                .findFirst();
    }

    /**
     * Get all available providers sorted by priority
     */
    private List<LLMProvider> getAvailableProvidersByPriority() {
        return providers.stream()
                .filter(LLMProvider::isAvailable)
                .sorted(Comparator.comparingInt(p -> p.getMetadata().getPriority()))
                .toList();
    }

    /**
     * Get status of all providers
     */
    public List<ProviderMetadata> getProvidersStatus() {
        return providers.stream()
                .map(LLMProvider::getMetadata)
                .toList();
    }
}
