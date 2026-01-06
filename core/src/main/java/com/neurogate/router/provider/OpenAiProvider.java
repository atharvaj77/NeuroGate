package com.neurogate.router.provider;

import com.neurogate.router.upstream.OpenAiClient;
import com.neurogate.sentinel.model.ChatRequest;
import com.neurogate.sentinel.model.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * OpenAI provider implementation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiProvider implements LLMProvider {

    private final OpenAiClient openAiClient;
    private final ChatModel chatModel;

    private static final Map<String, String> MODEL_EQUIVALENTS = Map.of(
            "gpt-4", "gpt-4",
            "gpt-4-turbo", "gpt-4-turbo",
            "gpt-3.5-turbo", "gpt-3.5-turbo",
            "claude-3-opus", "gpt-4", // Equivalent to Claude Opus
            "claude-3-sonnet", "gpt-3.5-turbo", // Equivalent to Claude Sonnet
            "gemini-ultra", "gpt-4", // Equivalent to Gemini Ultra
            "gemini-pro", "gpt-3.5-turbo" // Equivalent to Gemini Pro
    );

    @Override
    public String getName() {
        return "openai";
    }

    @Override
    public List<String> getSupportedModels() {
        return List.of(
                "gpt-4",
                "gpt-4-turbo",
                "gpt-4-turbo-preview",
                "gpt-3.5-turbo",
                "gpt-3.5-turbo-16k");
    }

    @Override
    public boolean isAvailable() {
        try {
            // Health check by checking if ChatModel is configured
            return chatModel != null;
        } catch (Exception e) {
            log.warn("OpenAI provider health check failed", e);
            return false;
        }
    }

    @Override
    public ProviderMetadata getMetadata() {
        return ProviderMetadata.builder()
                .name("openai")
                .priority(1) // Highest priority
                .enabled(true)
                .avgLatencyMs(500)
                .costPer1kInputTokens(new BigDecimal("0.03")) // GPT-4 pricing
                .costPer1kOutputTokens(new BigDecimal("0.06"))
                .maxTokens(128000)
                .supportsStreaming(true)
                .supportsFunctionCalling(true)
                .supportsVision(true)
                .maxRpm(10000)
                .healthCheckUrl("https://api.openai.com/v1/models")
                .build();
    }

    @Override
    public ChatResponse generate(ChatRequest request) {
        log.debug("OpenAI provider generating completion for model: {}", request.getModel());
        return openAiClient.generateCompletion(request);
    }

    @Override
    public Flux<ChatResponse> generateStream(ChatRequest request) {
        log.debug("OpenAI provider generating streaming completion for model: {}", request.getModel());
        return openAiClient.generateStream(request);
    }

    @Override
    public String getEquivalentModel(String requestedModel) {
        return MODEL_EQUIVALENTS.getOrDefault(requestedModel, "gpt-3.5-turbo");
    }
}
