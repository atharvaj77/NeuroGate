package com.neurogate.router.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neurogate.exception.ProviderException;
import com.neurogate.sentinel.model.ChatRequest;
import com.neurogate.sentinel.model.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * Abstract base class for LLM providers.
 * Provides common functionality for error handling, retry logic, and response mapping.
 */
@Slf4j
public abstract class AbstractLLMProvider implements LLMProvider {

    protected final WebClient.Builder webClientBuilder;
    protected final ObjectMapper objectMapper;

    protected AbstractLLMProvider(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClientBuilder = webClientBuilder;
        this.objectMapper = objectMapper;
    }

    /**
     * Template method for generating completions.
     * Handles common error handling and metrics.
     */
    @Override
    public ChatResponse generate(ChatRequest request) {
        validateAvailability();
        log.debug("{} provider generating completion for model: {}", getName(), request.getModel());

        long startTime = System.currentTimeMillis();
        try {
            ChatResponse response = doGenerate(request);
            long latency = System.currentTimeMillis() - startTime;
            response.setLatencyMs(latency);
            return response;
        } catch (WebClientResponseException e) {
            throw handleWebClientError(e);
        } catch (Exception e) {
            throw handleGeneralError(e);
        }
    }

    /**
     * Template method for streaming completions.
     */
    @Override
    public Flux<ChatResponse> generateStream(ChatRequest request) {
        validateAvailability();
        log.debug("{} provider generating streaming completion for model: {}", getName(), request.getModel());

        try {
            return doGenerateStream(request)
                    .onErrorResume(WebClientResponseException.class, e -> {
                        log.error("{} stream error: {} {}", getName(), e.getStatusCode(), e.getMessage());
                        return Flux.error(handleWebClientError(e));
                    });
        } catch (Exception e) {
            throw handleGeneralError(e);
        }
    }

    /**
     * Provider-specific implementation for non-streaming generation.
     */
    protected abstract ChatResponse doGenerate(ChatRequest request);

    /**
     * Provider-specific implementation for streaming generation.
     */
    protected abstract Flux<ChatResponse> doGenerateStream(ChatRequest request);

    /**
     * Get the API key for this provider.
     */
    protected abstract String getApiKey();

    /**
     * Get the base URL for this provider.
     */
    protected abstract String getBaseUrl();

    /**
     * Get model equivalents map for fallback routing.
     */
    protected abstract Map<String, String> getModelEquivalents();

    /**
     * Get the default model for this provider.
     */
    protected abstract String getDefaultModel();

    /**
     * Validate that the provider is available before making requests.
     */
    protected void validateAvailability() {
        if (!isAvailable()) {
            throw new ProviderException(getName(),
                    getName() + " provider is not configured. Set the API key.");
        }
    }

    /**
     * Handle WebClient errors and convert to ProviderException.
     */
    protected ProviderException handleWebClientError(WebClientResponseException e) {
        int statusCode = e.getStatusCode().value();
        String message = String.format("%s API error: %d - %s",
                getName(), statusCode, e.getMessage());
        log.error(message);
        return new ProviderException(getName(), message, statusCode);
    }

    /**
     * Handle general errors.
     */
    protected ProviderException handleGeneralError(Exception e) {
        String message = String.format("%s API call failed: %s", getName(), e.getMessage());
        log.error(message, e);
        return new ProviderException(getName(), message, e);
    }

    /**
     * Default implementation checks if API key is configured.
     */
    @Override
    public boolean isAvailable() {
        String apiKey = getApiKey();
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public String getEquivalentModel(String requestedModel) {
        return getModelEquivalents().getOrDefault(requestedModel, getDefaultModel());
    }

    /**
     * Generate a unique response ID.
     */
    protected String generateResponseId() {
        return "chatcmpl-" + getName() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Get default timeout duration.
     */
    protected Duration getTimeout() {
        return Duration.ofSeconds(30);
    }

    /**
     * Get default max tokens if not specified in request.
     */
    protected int getDefaultMaxTokens() {
        return 4096;
    }

    /**
     * Get default temperature if not specified in request.
     */
    protected double getDefaultTemperature() {
        return 0.7;
    }

    /**
     * Build a fallback response when circuit breaker triggers.
     */
    protected ChatResponse buildFallbackResponse(ChatRequest request, Throwable throwable) {
        log.warn("{} circuit breaker triggered, using fallback. Reason: {}",
                getName(), throwable.getMessage());

        return ChatResponse.builder()
                .id(generateResponseId() + "-fallback")
                .object("chat.completion")
                .created(System.currentTimeMillis() / 1000)
                .model(request.getModel())
                .choices(java.util.List.of(
                        com.neurogate.sentinel.model.Choice.builder()
                                .index(0)
                                .message(com.neurogate.sentinel.model.Message.builder()
                                        .role("assistant")
                                        .content("I apologize, but the " + getName() +
                                                " service is temporarily unavailable. Please try again.")
                                        .build())
                                .finishReason("error")
                                .build()))
                .usage(com.neurogate.sentinel.model.Usage.builder()
                        .promptTokens(0)
                        .completionTokens(0)
                        .totalTokens(0)
                        .build())
                .cacheHit(false)
                .route(getName() + "-fallback")
                .error(getName() + " service temporarily unavailable: " + throwable.getMessage())
                .build();
    }
}
