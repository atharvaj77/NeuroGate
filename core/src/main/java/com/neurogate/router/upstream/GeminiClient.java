package com.neurogate.router.upstream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neurogate.sentinel.model.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Client for interacting with Google Gemini API.
 * Handles HTTP communication and resilience patterns.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiClient {

        private final WebClient.Builder webClientBuilder;
        private final ObjectMapper objectMapper;
        private final io.micrometer.core.instrument.MeterRegistry meterRegistry;

        @Value("${neurogate.gemini.api-key:#{null}}")
        private String apiKey;

        @Value("${neurogate.gemini.base-url:https://generativelanguage.googleapis.com}")
        private String baseUrl;

        /**
         * Generate chat completion using Gemini with resilience patterns.
         */
        @CircuitBreaker(name = "gemini", fallbackMethod = "generateFallback")
        @Retry(name = "gemini")
        public ChatResponse generateCompletion(ChatRequest request, String model) {
                if (apiKey == null || apiKey.isBlank()) {
                        throw new IllegalStateException("Gemini API key is not configured");
                }

                log.debug("Sending request to Gemini: model={}", model);
                meterRegistry.counter("neurogate.upstream.requests", "provider", "gemini").increment();

                try {
                        WebClient client = webClientBuilder
                                        .baseUrl(baseUrl)
                                        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .build();

                        // Build Gemini API request
                        Map<String, Object> geminiRequest = Map.of(
                                        "contents", List.of(
                                                        Map.of(
                                                                        "parts", List.of(
                                                                                        Map.of("text", request
                                                                                                        .getConcatenatedContent())))),
                                        "generationConfig", Map.of(
                                                        "temperature",
                                                        request.getTemperature() != null ? request.getTemperature()
                                                                        : 0.7,
                                                        "maxOutputTokens",
                                                        request.getMaxTokens() != null ? request.getMaxTokens() : 2048,
                                                        "topP", request.getTopP() != null ? request.getTopP() : 0.95));

                        // Call Gemini API
                        Mono<String> responseMono = client.post()
                                        .uri("/v1beta/models/" + model + ":generateContent?key=" + apiKey)
                                        .bodyValue(geminiRequest)
                                        .retrieve()
                                        .bodyToMono(String.class)
                                        .timeout(Duration.ofSeconds(30));

                        String responseBody = responseMono.block();
                        ChatResponse response = convertToNeuroGateResponse(responseBody, request.getModel());

                        // Record cost (Estimate)
                        if (response.getUsage() != null) {
                                // Gemini Pro is roughly $0.000125/1k input, $0.000375/1k output (very cheap)
                                double cost = (response.getUsage().getPromptTokens() / 1000.0 * 0.000125) +
                                                (response.getUsage().getCompletionTokens() / 1000.0 * 0.000375);
                                response.setCostUsd(cost);
                                meterRegistry.counter("neurogate.upstream.cost", "provider", "gemini").increment(cost);
                        }

                        return response;

                } catch (Exception e) {
                        log.error("Error calling Gemini API", e);
                        meterRegistry.counter("neurogate.upstream.errors", "provider", "gemini").increment();
                        throw new RuntimeException("Gemini API call failed: " + e.getMessage(), e);
                }
        }

        /**
         * Convert Gemini API response to NeuroGate ChatResponse format.
         */
        private ChatResponse convertToNeuroGateResponse(String responseBody, String requestedModel) {
                try {
                        JsonNode root = objectMapper.readTree(responseBody);

                        // Extract content from Gemini response
                        JsonNode candidates = root.path("candidates");
                        if (candidates.isEmpty()) {
                                throw new RuntimeException("No candidates in Gemini response");
                        }

                        JsonNode firstCandidate = candidates.get(0);
                        JsonNode content = firstCandidate.path("content").path("parts").get(0);
                        String text = content.path("text").asText();

                        String finishReason = firstCandidate.path("finishReason").asText("STOP").toLowerCase();

                        // Extract token counts
                        int inputTokens = root.path("usageMetadata").path("promptTokenCount").asInt(0);
                        int outputTokens = root.path("usageMetadata").path("candidatesTokenCount").asInt(0);

                        return ChatResponse.builder()
                                        .id("chatcmpl-gemini-" + UUID.randomUUID().toString().substring(0, 8))
                                        .object("chat.completion")
                                        .created(System.currentTimeMillis() / 1000)
                                        .model(requestedModel)
                                        .choices(List.of(
                                                        Choice.builder()
                                                                        .index(0)
                                                                        .message(Message.builder()
                                                                                        .role("assistant")
                                                                                        .content(text)
                                                                                        .build())
                                                                        .finishReason(finishReason)
                                                                        .build()))
                                        .usage(Usage.builder()
                                                        .promptTokens(inputTokens)
                                                        .completionTokens(outputTokens)
                                                        .totalTokens(inputTokens + outputTokens)
                                                        .build())
                                        .cacheHit(false)
                                        .route("gemini")
                                        .build();

                } catch (Exception e) {
                        log.error("Error parsing Gemini response", e);
                        throw new RuntimeException("Failed to parse Gemini response: " + e.getMessage(), e);
                }
        }

        /**
         * Fallback method for circuit breaker.
         */
        private ChatResponse generateFallback(ChatRequest request, String model, Throwable throwable) {
                log.warn("Gemini circuit breaker triggered, using fallback. Reason: {}", throwable.getMessage());

                return ChatResponse.builder()
                                .id("chatcmpl-gemini-fallback-" + UUID.randomUUID().toString().substring(0, 8))
                                .object("chat.completion")
                                .created(System.currentTimeMillis() / 1000)
                                .model(request.getModel())
                                .choices(List.of(
                                                Choice.builder()
                                                                .index(0)
                                                                .message(Message.builder()
                                                                                .role("assistant")
                                                                                .content(
                                                                                                "I apologize, but the Google Gemini service is temporarily unavailable. Please try again in a moment.")
                                                                                .build())
                                                                .finishReason("error")
                                                                .build()))
                                .usage(Usage.builder()
                                                .promptTokens(0)
                                                .completionTokens(0)
                                                .totalTokens(0)
                                                .build())
                                .cacheHit(false)
                                .route("gemini-fallback")
                                .error("Gemini service temporarily unavailable: " + throwable.getMessage())
                                .build();
        }

        /**
         * Generate streaming chat completion using Gemini (SSE).
         */
        @CircuitBreaker(name = "gemini-stream", fallbackMethod = "generateStreamFallback")
        @Retry(name = "gemini-stream")
        public reactor.core.publisher.Flux<ChatResponse> generateStream(ChatRequest request, String model) {
                if (apiKey == null || apiKey.isBlank()) {
                        throw new IllegalStateException("Gemini API key is not configured");
                }

                log.debug("Sending streaming request to Gemini: model={}", model);

                try {
                        WebClient client = webClientBuilder
                                        .baseUrl(baseUrl)
                                        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .build();

                        // Build Gemini API request (same as completion)
                        Map<String, Object> geminiRequest = Map.of(
                                        "contents", List.of(
                                                        Map.of(
                                                                        "parts", List.of(
                                                                                        Map.of("text", request
                                                                                                        .getConcatenatedContent())))),
                                        "generationConfig", Map.of(
                                                        "temperature",
                                                        request.getTemperature() != null ? request.getTemperature()
                                                                        : 0.7,
                                                        "maxOutputTokens",
                                                        request.getMaxTokens() != null ? request.getMaxTokens() : 2048,
                                                        "topP", request.getTopP() != null ? request.getTopP() : 0.95));

                        // Call Gemini API with alt=sse
                        return client.post()
                                        .uri("/v1beta/models/" + model + ":streamGenerateContent?key=" + apiKey
                                                        + "&alt=sse")
                                        .bodyValue(geminiRequest)
                                        .retrieve()
                                        .bodyToFlux(String.class)
                                        .filter(line -> line.startsWith("data:"))
                                        .map(line -> line.substring(5).trim()) // Remove "data: "
                                        .map(json -> convertToNeuroGateStreamResponse(json, request.getModel()));

                } catch (Exception e) {
                        log.error("Error calling Gemini stream API", e);
                        throw new RuntimeException("Gemini stream API call failed: " + e.getMessage(), e);
                }
        }

        private ChatResponse convertToNeuroGateStreamResponse(String responseBody, String requestedModel) {
                try {
                        JsonNode root = objectMapper.readTree(responseBody);

                        // Extract content from Gemini stream chunk
                        JsonNode candidates = root.path("candidates");
                        if (candidates.isEmpty()) {
                                // Sometimes the last chunk is empty or just usage metadata, return empty delta?
                                return createEmptyStreamResponse(requestedModel);
                        }

                        JsonNode firstCandidate = candidates.get(0);
                        JsonNode contentNode = firstCandidate.path("content").path("parts").get(0);
                        String text = contentNode.path("text").asText("");

                        // usageMetadata might be present in the last chunk
                        JsonNode usageMetadata = root.path("usageMetadata");
                        Usage usage = null;
                        if (!usageMetadata.isMissingNode()) {
                                int inputTokens = usageMetadata.path("promptTokenCount").asInt(0);
                                int outputTokens = usageMetadata.path("candidatesTokenCount").asInt(0);
                                usage = Usage.builder()
                                                .promptTokens(inputTokens)
                                                .completionTokens(outputTokens)
                                                .totalTokens(inputTokens + outputTokens)
                                                .build();
                        }

                        return ChatResponse.builder()
                                        .id("chatcmpl-gemini-" + UUID.randomUUID().toString().substring(0, 8))
                                        .object("chat.completion.chunk")
                                        .created(System.currentTimeMillis() / 1000)
                                        .model(requestedModel)
                                        .choices(List.of(
                                                        Choice.builder()
                                                                        .index(0)
                                                                        .delta(Message.builder()
                                                                                        .role("assistant")
                                                                                        .content(text)
                                                                                        .build())
                                                                        .finishReason(null)
                                                                        .build()))
                                        .usage(usage)
                                        .cacheHit(false)
                                        .route("gemini")
                                        .build();

                } catch (Exception e) {
                        log.error("Error parsing Gemini stream response: {}", responseBody, e);
                        // Return empty response or rethrow? Better to swallow bad chunks in stream
                        return createEmptyStreamResponse(requestedModel);
                }
        }

        private ChatResponse createEmptyStreamResponse(String model) {
                return ChatResponse.builder()
                                .id("chatcmpl-gemini-empty-" + UUID.randomUUID().toString().substring(0, 8))
                                .object("chat.completion.chunk")
                                .created(System.currentTimeMillis() / 1000)
                                .model(model)
                                .choices(List.of())
                                .build();
        }

        private reactor.core.publisher.Flux<ChatResponse> generateStreamFallback(ChatRequest request, String model,
                        Throwable throwable) {
                log.warn("Gemini stream circuit breaker triggered, using fallback. Reason: {}", throwable.getMessage());
                return reactor.core.publisher.Flux.just(
                                ChatResponse.builder()
                                                .id("chatcmpl-gemini-fallback-"
                                                                + UUID.randomUUID().toString().substring(0, 8))
                                                .object("chat.completion.chunk")
                                                .created(System.currentTimeMillis() / 1000)
                                                .model(request.getModel())
                                                .choices(List.of(
                                                                Choice.builder()
                                                                                .index(0)
                                                                                .delta(Message.builder()
                                                                                                .role("assistant")
                                                                                                .content("I apologize, but the Google Gemini service is temporarily unavailable.")
                                                                                                .build())
                                                                                .finishReason("stop")
                                                                                .build()))
                                                .build());
        }
}
