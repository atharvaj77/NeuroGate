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
 * Azure OpenAI provider implementation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AzureOpenAiProvider implements LLMProvider {

        private final WebClient.Builder webClientBuilder;
        private final ObjectMapper objectMapper;

        @Value("${neurogate.azure.api-key:#{null}}")
        private String apiKey;

        @Value("${neurogate.azure.endpoint:#{null}}")
        private String endpoint; // https://<resource-name>.openai.azure.com

        @Value("${neurogate.azure.deployment-name:#{null}}")
        private String deploymentName; // Your deployment name

        @Value("${neurogate.azure.api-version:2024-02-15-preview}")
        private String apiVersion;

        private static final Map<String, String> MODEL_EQUIVALENTS = Map.of(
                        "gpt-4", "gpt-4",
                        "gpt-4-turbo", "gpt-4-turbo",
                        "gpt-3.5-turbo", "gpt-35-turbo", // Azure uses different naming
                        "claude-3-opus", "gpt-4",
                        "gemini-pro", "gpt-35-turbo");

        @Override
        public String getName() {
                return "azure";
        }

        @Override
        public List<String> getSupportedModels() {
                return List.of(
                                "gpt-4",
                                "gpt-4-turbo",
                                "gpt-35-turbo",
                                "gpt-35-turbo-16k");
        }

        @Override
        public boolean isAvailable() {
                return apiKey != null && !apiKey.isBlank()
                                && endpoint != null && !endpoint.isBlank()
                                && deploymentName != null && !deploymentName.isBlank();
        }

        @Override
        public ProviderMetadata getMetadata() {
                return ProviderMetadata.builder()
                                .name("azure")
                                .priority(5) // Lowest priority (after OpenAI, Anthropic, Gemini, Bedrock)
                                .enabled(isAvailable())
                                .avgLatencyMs(550)
                                .costPer1kInputTokens(new BigDecimal("0.03")) // GPT-4 pricing on Azure
                                .costPer1kOutputTokens(new BigDecimal("0.06"))
                                .maxTokens(128000)
                                .supportsStreaming(true)
                                .supportsFunctionCalling(true)
                                .supportsVision(true)
                                .maxRpm(10000)
                                .healthCheckUrl(endpoint + "/openai/deployments")
                                .build();
        }

        @Override
        @CircuitBreaker(name = "azure", fallbackMethod = "generateFallback")
        @Retry(name = "azure")
        public ChatResponse generate(ChatRequest request) {
                if (!isAvailable()) {
                        throw new IllegalStateException(
                                        "Azure OpenAI provider is not configured. Set API key, endpoint, and deployment name.");
                }

                log.debug("Azure OpenAI provider generating completion for model: {}", request.getModel());

                try {
                        WebClient client = webClientBuilder
                                        .baseUrl(endpoint)
                                        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .defaultHeader("api-key", apiKey)
                                        .build();

                        // Build Azure OpenAI request (similar to OpenAI but with Azure-specific URL)
                        Map<String, Object> azureRequest = Map.of(
                                        "messages", List.of(
                                                        Map.of(
                                                                        "role", "user",
                                                                        "content", request.getConcatenatedContent())),
                                        "temperature",
                                        request.getTemperature() != null ? request.getTemperature() : 0.7,
                                        "max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : 4096,
                                        "top_p", request.getTopP() != null ? request.getTopP() : 1.0);

                        // Call Azure OpenAI API
                        // URL format:
                        // /openai/deployments/{deployment-id}/chat/completions?api-version={api-version}
                        String uri = String.format("/openai/deployments/%s/chat/completions?api-version=%s",
                                        deploymentName, apiVersion);

                        Mono<String> responseMono = client.post()
                                        .uri(uri)
                                        .bodyValue(azureRequest)
                                        .retrieve()
                                        .bodyToMono(String.class)
                                        .timeout(Duration.ofSeconds(30));

                        String responseBody = responseMono.block();
                        return convertToNeuroGateResponse(responseBody, request.getModel());

                } catch (Exception e) {
                        log.error("Error calling Azure OpenAI API", e);
                        throw new RuntimeException("Azure OpenAI API call failed: " + e.getMessage(), e);
                }
        }

        @Override
        @CircuitBreaker(name = "azure-stream", fallbackMethod = "generateStreamFallback")
        @Retry(name = "azure-stream")
        public Flux<ChatResponse> generateStream(ChatRequest request) {
                if (!isAvailable()) {
                        throw new IllegalStateException(
                                        "Azure OpenAI provider is not configured. Set API key, endpoint, and deployment name.");
                }

                log.debug("Azure OpenAI provider generating streaming completion for model: {}", request.getModel());

                try {
                        WebClient client = webClientBuilder
                                        .baseUrl(endpoint)
                                        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .defaultHeader("api-key", apiKey)
                                        .build();

                        // Build Azure OpenAI request (similar to OpenAI but with Azure-specific URL)
                        Map<String, Object> azureRequest = Map.of(
                                        "messages", List.of(
                                                        Map.of(
                                                                        "role", "user",
                                                                        "content", request.getConcatenatedContent())),
                                        "temperature",
                                        request.getTemperature() != null ? request.getTemperature() : 0.7,
                                        "max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : 4096,
                                        "top_p", request.getTopP() != null ? request.getTopP() : 1.0,
                                        "stream", true);

                        // Call Azure OpenAI API
                        // URL format:
                        // /openai/deployments/{deployment-id}/chat/completions?api-version={api-version}
                        String uri = String.format("/openai/deployments/%s/chat/completions?api-version=%s",
                                        deploymentName, apiVersion);

                        return client.post()
                                        .uri(uri)
                                        .bodyValue(azureRequest)
                                        .retrieve()
                                        .bodyToFlux(String.class)
                                        .filter(line -> line.startsWith("data:"))
                                        .map(line -> line.substring(5).trim()) // Remove "data: "
                                        .filter(json -> !json.equals("[DONE]"))
                                        .map(json -> convertToNeuroGateStreamResponse(json, request.getModel()));

                } catch (Exception e) {
                        log.error("Error calling Azure OpenAI stream API", e);
                        throw new RuntimeException("Azure OpenAI stream API call failed: " + e.getMessage(), e);
                }
        }

        private ChatResponse convertToNeuroGateStreamResponse(String responseBody, String requestedModel) {
                try {
                        JsonNode root = objectMapper.readTree(responseBody);

                        // Extract content from Azure OpenAI chunk (same as OpenAI)
                        String content = "";
                        String finishReason = null;

                        JsonNode choices = root.path("choices");
                        if (!choices.isEmpty()) {
                                JsonNode firstChoice = choices.get(0);
                                content = firstChoice.path("delta").path("content").asText("");
                                finishReason = firstChoice.path("finish_reason").asText(null);
                        }

                        return ChatResponse.builder()
                                        .id("chatcmpl-azure-" + UUID.randomUUID().toString().substring(0, 8))
                                        .object("chat.completion.chunk")
                                        .created(System.currentTimeMillis() / 1000)
                                        .model(requestedModel)
                                        .choices(List.of(
                                                        Choice.builder()
                                                                        .index(0)
                                                                        .delta(Message.builder()
                                                                                        .role("assistant")
                                                                                        .content(content)
                                                                                        .build())
                                                                        .finishReason(finishReason)
                                                                        .build()))
                                        .usage(null)
                                        .cacheHit(false)
                                        .route("azure")
                                        .build();

                } catch (Exception e) {
                        return ChatResponse.builder().id("error").choices(List.of()).build();
                }
        }

        private Flux<ChatResponse> generateStreamFallback(ChatRequest request, Throwable throwable) {
                log.warn("Azure OpenAI stream circuit breaker triggered, using fallback. Reason: {}",
                                throwable.getMessage());
                return Flux.just(
                                ChatResponse.builder()
                                                .id("chatcmpl-azure-fallback-"
                                                                + UUID.randomUUID().toString().substring(0, 8))
                                                .object("chat.completion.chunk")
                                                .created(System.currentTimeMillis() / 1000)
                                                .model(request.getModel())
                                                .choices(List.of(
                                                                Choice.builder()
                                                                                .index(0)
                                                                                .delta(Message.builder()
                                                                                                .role("assistant")
                                                                                                .content("I apologize, but the Azure OpenAI service is temporarily unavailable.")
                                                                                                .build())
                                                                                .finishReason("stop")
                                                                                .build()))
                                                .build());
        }

        @Override
        public String getEquivalentModel(String requestedModel) {
                return MODEL_EQUIVALENTS.getOrDefault(requestedModel, "gpt-35-turbo");
        }

        /**
         * Convert Azure OpenAI response to NeuroGate ChatResponse format
         * (Azure OpenAI uses the same format as OpenAI)
         */
        private ChatResponse convertToNeuroGateResponse(String responseBody, String requestedModel) {
                try {
                        JsonNode root = objectMapper.readTree(responseBody);

                        // Extract content from Azure OpenAI response (same format as OpenAI)
                        String content = root.path("choices").get(0)
                                        .path("message").path("content").asText();

                        String finishReason = root.path("choices").get(0)
                                        .path("finish_reason").asText("stop");

                        // Extract usage stats
                        int inputTokens = root.path("usage").path("prompt_tokens").asInt(0);
                        int outputTokens = root.path("usage").path("completion_tokens").asInt(0);

                        return ChatResponse.builder()
                                        .id("chatcmpl-azure-" + UUID.randomUUID().toString().substring(0, 8))
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
                                                                        .finishReason(finishReason)
                                                                        .build()))
                                        .usage(Usage.builder()
                                                        .promptTokens(inputTokens)
                                                        .completionTokens(outputTokens)
                                                        .totalTokens(inputTokens + outputTokens)
                                                        .build())
                                        .cacheHit(false)
                                        .route("azure")
                                        .build();

                } catch (Exception e) {
                        log.error("Error parsing Azure OpenAI response", e);
                        throw new RuntimeException("Failed to parse Azure OpenAI response: " + e.getMessage(), e);
                }
        }

        /**
         * Fallback method for circuit breaker
         */
        private ChatResponse generateFallback(ChatRequest request, Throwable throwable) {
                log.warn("Azure OpenAI circuit breaker triggered, using fallback. Reason: {}", throwable.getMessage());

                return ChatResponse.builder()
                                .id("chatcmpl-azure-fallback-" + UUID.randomUUID().toString().substring(0, 8))
                                .object("chat.completion")
                                .created(System.currentTimeMillis() / 1000)
                                .model(request.getModel())
                                .choices(List.of(
                                                Choice.builder()
                                                                .index(0)
                                                                .message(Message.builder()
                                                                                .role("assistant")
                                                                                .content("I apologize, but the Azure OpenAI service is temporarily unavailable. Please try again in a moment.")
                                                                                .build())
                                                                .finishReason("error")
                                                                .build()))
                                .usage(Usage.builder()
                                                .promptTokens(0)
                                                .completionTokens(0)
                                                .totalTokens(0)
                                                .build())
                                .cacheHit(false)
                                .route("azure-fallback")
                                .error("Azure OpenAI service temporarily unavailable: " + throwable.getMessage())
                                .build();
        }
}
