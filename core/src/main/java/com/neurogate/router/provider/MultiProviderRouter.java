package com.neurogate.router.provider;

import com.neurogate.metrics.NeuroGateMetrics;
import com.neurogate.router.resilience.ResilienceService;
import com.neurogate.sentinel.model.ChatRequest;
import com.neurogate.sentinel.model.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Intelligently routes requests to the best available LLM provider.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MultiProviderRouter {

    private final List<LLMProvider> providers;
    private final NeuroGateMetrics metrics;
    private final ResilienceService resilienceService;

    public ChatResponse route(ChatRequest request) {
        String requestedModel = request.getModel();
        log.debug("Multi-provider routing request for model: {}", requestedModel);

        Optional<LLMProvider> directProvider = findProviderForModel(requestedModel);

        if (directProvider.isPresent() && directProvider.get().isAvailable()) {
            LLMProvider provider = directProvider.get();
            log.info("Routing to direct provider: {} for model: {}", provider.getName(), requestedModel);
            metrics.recordProviderRequest(provider.getName());

            // Execute with circuit breaker and retry, fallback to other providers
            return resilienceService.execute(
                    provider.getName(),
                    () -> provider.generate(request),
                    throwable -> {
                        log.warn("Direct provider {} failed with resilience: {}", provider.getName(),
                                throwable.getMessage());
                        metrics.recordProviderFailure(provider.getName());
                        return routeToFallbackProviders(request, requestedModel, provider.getName());
                    });
        }

        // No direct provider, go straight to fallbacks
        return routeToFallbackProviders(request, requestedModel, null);
    }

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

        throw new RuntimeException("All LLM providers failed. OpenAI, Anthropic, and Gemini are unavailable.");
    }

    public Flux<ChatResponse> routeStream(ChatRequest request) {
        String requestedModel = request.getModel();
        log.debug("Multi-provider routing streaming request for model: {}", requestedModel);

        // Try to find a provider that directly supports the requested model
        Optional<LLMProvider> directProvider = findProviderForModel(requestedModel);

        if (directProvider.isPresent() && directProvider.get().isAvailable()) {
            try {
                log.info("Routing streaming to direct provider: {} for model: {}",
                        directProvider.get().getName(), requestedModel);
                return directProvider.get().generateStream(request);
            } catch (Exception e) {
                log.warn("Direct provider {} streaming failed, trying fallback: {}",
                        directProvider.get().getName(), e.getMessage());
            }
        }

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

        return Flux.error(new RuntimeException("All LLM providers failed for streaming"));
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
