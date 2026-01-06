package com.neurogate.router.provider;

import com.neurogate.router.upstream.GeminiClient;
import com.neurogate.sentinel.model.ChatRequest;
import com.neurogate.sentinel.model.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Google Gemini provider implementation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiProvider implements LLMProvider {

        private final GeminiClient geminiClient;

        @org.springframework.beans.factory.annotation.Value("${neurogate.gemini.api-key:#{null}}")
        private String apiKey;

        @org.springframework.beans.factory.annotation.Value("${neurogate.gemini.base-url:https://generativelanguage.googleapis.com}")
        private String baseUrl;

        private static final Map<String, String> MODEL_EQUIVALENTS = Map.of(
                        "gpt-4", "gemini-pro",
                        "gpt-4-turbo", "gemini-pro",
                        "gpt-3.5-turbo", "gemini-pro",
                        "claude-3-opus", "gemini-pro",
                        "claude-3-sonnet", "gemini-pro",
                        "gemini-pro", "gemini-pro",
                        "gemini-ultra", "gemini-ultra");

        @Override
        public String getName() {
                return "gemini";
        }

        @Override
        public List<String> getSupportedModels() {
                return List.of(
                                "gemini-pro",
                                "gemini-ultra",
                                "gemini-pro-vision");
        }

        @Override
        public boolean isAvailable() {
                return apiKey != null && !apiKey.isBlank();
        }

        @Override
        public ProviderMetadata getMetadata() {
                return ProviderMetadata.builder()
                                .name("gemini")
                                .priority(3) // Third priority
                                .enabled(isAvailable())
                                .avgLatencyMs(550)
                                .costPer1kInputTokens(new BigDecimal("0.00025")) // Gemini Pro pricing (very cheap!)
                                .costPer1kOutputTokens(new BigDecimal("0.0005"))
                                .maxTokens(30720) // 32K context window
                                .supportsStreaming(true)
                                .supportsFunctionCalling(true)
                                .supportsVision(true)
                                .maxRpm(60) // Free tier: 60 RPM
                                .healthCheckUrl(baseUrl + "/v1beta/models")
                                .build();
        }

        @Override
        public ChatResponse generate(ChatRequest request) {
                if (!isAvailable()) {
                        throw new IllegalStateException(
                                        "Gemini provider is not configured. Set neurogate.gemini.api-key");
                }

                log.debug("Gemini provider generating completion for model: {}", request.getModel());
                String model = getEquivalentModel(request.getModel());
                return geminiClient.generateCompletion(request, model);
        }

        @Override
        public Flux<ChatResponse> generateStream(ChatRequest request) {
                if (!isAvailable()) {
                        throw new IllegalStateException(
                                        "Gemini provider is not configured. Set neurogate.gemini.api-key");
                }

                log.debug("Gemini provider generating streaming completion for model: {}", request.getModel());
                String model = getEquivalentModel(request.getModel());
                return geminiClient.generateStream(request, model);
        }

        @Override
        public String getEquivalentModel(String requestedModel) {
                return MODEL_EQUIVALENTS.getOrDefault(requestedModel, "gemini-pro");
        }
}
