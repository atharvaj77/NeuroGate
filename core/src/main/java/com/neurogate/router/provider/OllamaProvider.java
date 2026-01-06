package com.neurogate.router.provider;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.neurogate.sentinel.model.ChatRequest;
import com.neurogate.sentinel.model.ChatResponse;
import com.neurogate.sentinel.model.Choice;
import com.neurogate.sentinel.model.Message;
import com.neurogate.sentinel.model.Usage;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Ollama provider implementation for local LLM support.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OllamaProvider implements LLMProvider {

    @Value("${neurogate.ollama.base-url:http://localhost:11434}")
    private String baseUrl;

    @Value("${neurogate.ollama.enabled:true}")
    private boolean enabled;

    private final WebClient.Builder webClientBuilder;

    @Override
    public String getName() {
        return "ollama";
    }

    @Override
    public List<String> getSupportedModels() {
        if (!isAvailable()) {
            return Collections.emptyList();
        }
        try {
            OllamaTagsResponse response = webClientBuilder.build()
                    .get()
                    .uri(baseUrl + "/api/tags")
                    .retrieve()
                    .bodyToMono(OllamaTagsResponse.class)
                    .block(); // Block for simplicity in non-reactive context, ideally cache this

            if (response != null && response.getModels() != null) {
                return response.getModels().stream()
                        .map(OllamaModel::getName)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("Failed to fetch Ollama models", e);
        }
        return Collections.emptyList();
    }

    @Override
    public boolean isAvailable() {
        if (!enabled)
            return false;
        try {
            // Simple health check via version endpoint
            String version = webClientBuilder.build()
                    .get()
                    .uri(baseUrl + "/api/version")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            return version != null;
        } catch (Exception e) {
            log.warn("Ollama is not available at {}", baseUrl);
            return false;
        }
    }

    @Override
    public ProviderMetadata getMetadata() {
        return ProviderMetadata.builder()
                .name("ollama")
                .priority(10) // Lower priority than cloud providers by default
                .enabled(enabled)
                .avgLatencyMs(2000) // Local inference might be slower
                .costPer1kInputTokens(BigDecimal.ZERO) // Free!
                .costPer1kOutputTokens(BigDecimal.ZERO)
                .maxTokens(4096) // Typical for Llama models
                .supportsStreaming(true)
                .supportsFunctionCalling(false) // Basic support
                .supportsVision(false) // Depends on model
                .maxRpm(100) // Limited by local hardware
                .healthCheckUrl(baseUrl + "/api/version")
                .build();
    }

    @Override
    public ChatResponse generate(ChatRequest request) {
        log.debug("Ollama provider generating completion for model: {}", request.getModel());

        long startTime = System.currentTimeMillis();

        OllamaChatRequest ollamaRequest = mapToOllamaRequest(request, false);

        OllamaChatResponse ollamaResponse = webClientBuilder.build()
                .post()
                .uri(baseUrl + "/api/chat")
                .bodyValue(ollamaRequest)
                .retrieve()
                .bodyToMono(OllamaChatResponse.class)
                .block();

        return mapToChatResponse(ollamaResponse, request.getModel(), startTime);
    }

    @Override
    public Flux<ChatResponse> generateStream(ChatRequest request) {
        log.debug("Ollama provider generating streaming completion for model: {}", request.getModel());

        long startTime = System.currentTimeMillis();
        OllamaChatRequest ollamaRequest = mapToOllamaRequest(request, true);

        return webClientBuilder.build()
                .post()
                .uri(baseUrl + "/api/chat")
                .bodyValue(ollamaRequest)
                .retrieve()
                .bodyToFlux(OllamaChatResponse.class)
                .map(chunk -> mapToChatResponseChunk(chunk, request.getModel(), startTime));
    }

    @Override
    public String getEquivalentModel(String requestedModel) {
        // Simple heuristic: if it contains "llama" or local model names, return as is.
        // Otherwise, map strict cloud names to a default local fallback if needed.
        if (requestedModel.startsWith("gpt-")) {
            return "llama3"; // Default fallback for local dev
        }
        return requestedModel;
    }

    // --- Mappers ---

    private OllamaChatRequest mapToOllamaRequest(ChatRequest request, boolean stream) {
        return OllamaChatRequest.builder()
                .model(request.getModel())
                .messages(request.getMessages())
                .stream(stream)
                .options(Map.of(
                        "temperature", request.getTemperature() != null ? request.getTemperature() : 0.7,
                        "top_p", request.getTopP() != null ? request.getTopP() : 0.9))
                .build();
    }

    private ChatResponse mapToChatResponse(OllamaChatResponse ollamaResponse, String model, long startTime) {
        if (ollamaResponse == null)
            return null;

        Message message = ollamaResponse.getMessage();

        return ChatResponse.builder()
                .id(UUID.randomUUID().toString())
                .object("chat.completion")
                .created(Instant.now().getEpochSecond())
                .model(model)
                .choices(List.of(Choice.builder()
                        .index(0)
                        .message(message)
                        .finishReason(ollamaResponse.getDone() ? "stop" : null)
                        .build()))
                .usage(Usage.builder()
                        .promptTokens(ollamaResponse.getPromptEvalCount())
                        .completionTokens(ollamaResponse.getEvalCount())
                        .totalTokens(
                                (ollamaResponse.getPromptEvalCount() != null ? ollamaResponse.getPromptEvalCount() : 0)
                                        +
                                        (ollamaResponse.getEvalCount() != null ? ollamaResponse.getEvalCount() : 0))
                        .build())
                .latencyMs(System.currentTimeMillis() - startTime)
                .route("ollama")
                .costUsd(0.0)
                .build();
    }

    private ChatResponse mapToChatResponseChunk(OllamaChatResponse ollamaChunk, String model, long startTime) {
        if (ollamaChunk == null)
            return null;

        Message message = ollamaChunk.getMessage();

        return ChatResponse.builder()
                .id(UUID.randomUUID().toString())
                .object("chat.completion.chunk")
                .created(Instant.now().getEpochSecond())
                .model(model)
                .choices(List.of(Choice.builder()
                        .index(0)
                        .message(message) // Note: In streaming, this might need delta handling depending on upstream
                                          // consumer expectation
                        .finishReason(ollamaChunk.getDone() ? "stop" : null)
                        .build()))
                .latencyMs(System.currentTimeMillis() - startTime)
                .route("ollama")
                .build();
    }

    // --- Internal DTOs ---

    @Data
    @Builder
    private static class OllamaChatRequest {
        private String model;
        private List<Message> messages;
        private boolean stream;
        private Map<String, Object> options;
    }

    @Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class OllamaChatResponse {
        private String model;
        @JsonProperty("created_at")
        private String createdAt;
        private Message message;
        private Boolean done;
        @JsonProperty("prompt_eval_count")
        private Integer promptEvalCount;
        @JsonProperty("eval_count")
        private Integer evalCount;
    }

    @Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class OllamaTagsResponse {
        private List<OllamaModel> models;
    }

    @Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class OllamaModel {
        private String name;
        @JsonProperty("modified_at")
        private String modifiedAt;
        private long size;
    }
}
