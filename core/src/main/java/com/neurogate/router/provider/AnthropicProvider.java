package com.neurogate.router.provider;

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
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Anthropic Claude provider implementation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnthropicProvider implements LLMProvider {

        private final WebClient.Builder webClientBuilder;
        private final ObjectMapper objectMapper;

        @Value("${neurogate.anthropic.api-key:#{null}}")
        private String apiKey;

        @Value("${neurogate.anthropic.base-url:https://api.anthropic.com}")
        private String baseUrl;

        private static final String API_VERSION = "2023-06-01";

        private static final Map<String, String> MODEL_EQUIVALENTS = Map.of(
                        "gpt-4", "claude-3-opus-20240229",
                        "gpt-4-turbo", "claude-3-opus-20240229",
                        "gpt-3.5-turbo", "claude-3-sonnet-20240229",
                        "claude-3-opus", "claude-3-opus-20240229",
                        "claude-3-sonnet", "claude-3-sonnet-20240229",
                        "claude-3-haiku", "claude-3-haiku-20240307");

        @Override
        public String getName() {
                return "anthropic";
        }

        @Override
        public List<String> getSupportedModels() {
                return List.of(
                                "claude-3-opus-20240229",
                                "claude-3-sonnet-20240229",
                                "claude-3-haiku-20240307");
        }

        @Override
        public boolean isAvailable() {
                return apiKey != null && !apiKey.isBlank();
        }

        @Override
        public ProviderMetadata getMetadata() {
                return ProviderMetadata.builder()
                                .name("anthropic")
                                .priority(2) // Second priority (fallback after OpenAI)
                                .enabled(isAvailable())
                                .avgLatencyMs(600)
                                .costPer1kInputTokens(new BigDecimal("0.015")) // Claude 3 Opus pricing
                                .costPer1kOutputTokens(new BigDecimal("0.075"))
                                .maxTokens(200000) // Claude has 200K context window
                                .supportsStreaming(true)
                                .supportsFunctionCalling(true)
                                .supportsVision(true)
                                .maxRpm(4000)
                                .healthCheckUrl(baseUrl + "/v1/models")
                                .build();
        }

        @Override
        @CircuitBreaker(name = "anthropic", fallbackMethod = "generateFallback")
        @Retry(name = "anthropic")
        public ChatResponse generate(ChatRequest request) {
                if (!isAvailable()) {
                        throw new IllegalStateException(
                                        "Anthropic provider is not configured. Set neurogate.anthropic.api-key");
                }

                log.debug("Anthropic provider generating completion for model: {}", request.getModel());

                try {
                        WebClient client = webClientBuilder
                                        .baseUrl(baseUrl)
                                        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .defaultHeader("x-api-key", apiKey)
                                        .defaultHeader("anthropic-version", API_VERSION)
                                        .build();

                        // Build Anthropic API request
                        Map<String, Object> anthropicRequest = Map.of(
                                        "model", getEquivalentModel(request.getModel()),
                                        "max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : 4096,
                                        "messages", List.of(
                                                        Map.of(
                                                                        "role", "user",
                                                                        "content", request.getConcatenatedContent())),
                                        "temperature",
                                        request.getTemperature() != null ? request.getTemperature() : 0.7);

                        // Call Anthropic API
                        Mono<String> responseMono = client.post()
                                        .uri("/v1/messages")
                                        .bodyValue(anthropicRequest)
                                        .retrieve()
                                        .bodyToMono(String.class)
                                        .timeout(Duration.ofSeconds(30));

                        String responseBody = responseMono.block();
                        return convertToNeuroGateResponse(responseBody, request.getModel());

                } catch (Exception e) {
                        log.error("Error calling Anthropic API", e);
                        throw new RuntimeException("Anthropic API call failed: " + e.getMessage(), e);
                }
        }

        @Override
        @CircuitBreaker(name = "anthropic-stream", fallbackMethod = "generateStreamFallback")
        @Retry(name = "anthropic-stream")
        public Flux<ChatResponse> generateStream(ChatRequest request) {
                if (!isAvailable()) {
                        throw new IllegalStateException(
                                        "Anthropic provider is not configured. Set neurogate.anthropic.api-key");
                }

                log.debug("Anthropic provider generating streaming completion for model: {}", request.getModel());

                try {
                        WebClient client = webClientBuilder
                                        .baseUrl(baseUrl)
                                        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .defaultHeader("x-api-key", apiKey)
                                        .defaultHeader("anthropic-version", API_VERSION)
                                        .build();

                        // Build Anthropic API request
                        Map<String, Object> anthropicRequest = Map.of(
                                        "model", getEquivalentModel(request.getModel()),
                                        "max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : 4096,
                                        "messages", List.of(
                                                        Map.of(
                                                                        "role", "user",
                                                                        "content", request.getConcatenatedContent())),
                                        "temperature",
                                        request.getTemperature() != null ? request.getTemperature() : 0.7,
                                        "stream", true);

                        // Call Anthropic API with streaming
                        return client.post()
                                        .uri("/v1/messages")
                                        .bodyValue(anthropicRequest)
                                        .retrieve()
                                        .bodyToFlux(String.class)
                                        .filter(line -> line.startsWith("data:"))
                                        .map(line -> line.substring(5).trim()) // Remove "data: "
                                        .filter(json -> !json.equals("[DONE]"))
                                        .map(json -> convertToNeuroGateStreamResponse(json, request.getModel()));

                } catch (Exception e) {
                        log.error("Error calling Anthropic stream API", e);
                        throw new RuntimeException("Anthropic stream API call failed: " + e.getMessage(), e);
                }
        }

        private ChatResponse convertToNeuroGateStreamResponse(String responseBody, String requestedModel) {
                try {
                        JsonNode root = objectMapper.readTree(responseBody);
                        String type = root.path("type").asText();

                        String content = "";
                        String finishReason = null;
                        Usage usage = null;

                        if ("content_block_delta".equals(type)) {
                                content = root.path("delta").path("text").asText("");
                        } else if ("message_delta".equals(type)) {
                                finishReason = root.path("delta").path("stop_reason").asText(null);
                                // Usage might be here if Anthropic sends it in delta, but usually in
                                // message_stop
                        } else if ("message_start".equals(type)) {
                                // Initial message metadata
                                // int inputTokens =
                                // root.path("message").path("usage").path("input_tokens").asInt(0);
                        }

                        // Simple handling: only emit content deltas or finish reason
                        return ChatResponse.builder()
                                        .id("chatcmpl-anthropic-" + UUID.randomUUID().toString().substring(0, 8))
                                        .object("chat.completion.chunk")
                                        .created(System.currentTimeMillis() / 1000)
                                        .model(requestedModel)
                                        .choices(List.of(
                                                        Choice.builder()
                                                                        .index(0)
                                                                        .delta(Message.builder()
                                                                                        .role("assistant") // Only
                                                                                                           // needed for
                                                                                                           // first
                                                                                                           // chunk but
                                                                                                           // okay to
                                                                                                           // repeat
                                                                                        .content(content)
                                                                                        .build())
                                                                        .finishReason(finishReason)
                                                                        .build()))
                                        .usage(null) // Usage tracking in stream is complex, omitting for now
                                        .cacheHit(false)
                                        .route("anthropic")
                                        .build();

                } catch (Exception e) {
                        // log.error("Error parsing stream chunk", e);
                        // Returning empty response to keep stream alive
                        return ChatResponse.builder().id("error").choices(List.of()).build();
                }
        }

        private Flux<ChatResponse> generateStreamFallback(ChatRequest request, Throwable throwable) {
                log.warn("Anthropic stream circuit breaker triggered, using fallback. Reason: {}",
                                throwable.getMessage());
                return Flux.just(
                                ChatResponse.builder()
                                                .id("chatcmpl-anthropic-fallback-"
                                                                + UUID.randomUUID().toString().substring(0, 8))
                                                .object("chat.completion.chunk")
                                                .created(System.currentTimeMillis() / 1000)
                                                .model(request.getModel())
                                                .choices(List.of(
                                                                Choice.builder()
                                                                                .index(0)
                                                                                .delta(Message.builder()
                                                                                                .role("assistant")
                                                                                                .content("I apologize, but the Anthropic Claude service is temporarily unavailable.")
                                                                                                .build())
                                                                                .finishReason("stop")
                                                                                .build()))
                                                .build());
        }

        @Override
        public String getEquivalentModel(String requestedModel) {
                return MODEL_EQUIVALENTS.getOrDefault(requestedModel, "claude-3-sonnet-20240229");
        }

        /**
         * Convert Anthropic API response to NeuroGate ChatResponse format
         */
        private ChatResponse convertToNeuroGateResponse(String responseBody, String requestedModel) {
                try {
                        JsonNode root = objectMapper.readTree(responseBody);

                        // Extract content from Anthropic response
                        String content = root.path("content").get(0).path("text").asText();
                        String stopReason = root.path("stop_reason").asText("stop");

                        // Extract usage stats
                        int inputTokens = root.path("usage").path("input_tokens").asInt(0);
                        int outputTokens = root.path("usage").path("output_tokens").asInt(0);

                        return ChatResponse.builder()
                                        .id("chatcmpl-anthropic-" + UUID.randomUUID().toString().substring(0, 8))
                                        .object("chat.completion")
                                        .created(System.currentTimeMillis() / 1000)
                                        .model(requestedModel)
                                        .choices(List.of(
                                                        Choice.builder()
                                                                        .index(0)
                                                                        .message(Message.builder()
                                                                                        .role("assistant")
                                                                                        .content(content)
                                                                                        .build())
                                                                        .finishReason(stopReason)
                                                                        .build()))
                                        .usage(Usage.builder()
                                                        .promptTokens(inputTokens)
                                                        .completionTokens(outputTokens)
                                                        .totalTokens(inputTokens + outputTokens)
                                                        .build())
                                        .cacheHit(false)
                                        .route("anthropic")
                                        .build();

                } catch (Exception e) {
                        log.error("Error parsing Anthropic response", e);
                        throw new RuntimeException("Failed to parse Anthropic response: " + e.getMessage(), e);
                }
        }

        private ChatResponse generateFallback(ChatRequest request, Throwable throwable) {
                log.warn("Anthropic circuit breaker triggered, using fallback. Reason: {}", throwable.getMessage());

                return ChatResponse.builder()
                                .id("chatcmpl-anthropic-fallback-" + UUID.randomUUID().toString().substring(0, 8))
                                .object("chat.completion")
                                .created(System.currentTimeMillis() / 1000)
                                .model(request.getModel())
                                .choices(List.of(
                                                Choice.builder()
                                                                .index(0)
                                                                .message(Message.builder()
                                                                                .role("assistant")
                                                                                .content("I apologize, but the Anthropic Claude service is temporarily unavailable. Please try again in a moment.")
                                                                                .build())
                                                                .finishReason("error")
                                                                .build()))
                                .usage(Usage.builder()
                                                .promptTokens(0)
                                                .completionTokens(0)
                                                .totalTokens(0)
                                                .build())
                                .cacheHit(false)
                                .route("anthropic-fallback")
                                .error("Anthropic service temporarily unavailable: " + throwable.getMessage())
                                .build();
        }
}
