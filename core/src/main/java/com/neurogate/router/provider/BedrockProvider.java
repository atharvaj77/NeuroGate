package com.neurogate.router.provider;

import com.neurogate.router.upstream.BedrockClient;
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
 * AWS Bedrock provider implementation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BedrockProvider implements LLMProvider {

    private final BedrockClient bedrockClient;

    @org.springframework.beans.factory.annotation.Value("${neurogate.bedrock.access-key:#{null}}")
    private String accessKey;

    @org.springframework.beans.factory.annotation.Value("${neurogate.bedrock.secret-key:#{null}}")
    private String secretKey;

    @org.springframework.beans.factory.annotation.Value("${neurogate.bedrock.region:us-east-1}")
    private String region;

    private static final Map<String, String> MODEL_EQUIVALENTS = Map.of(
            "gpt-4", "anthropic.claude-3-opus-20240229-v1:0",
            "gpt-3.5-turbo", "anthropic.claude-3-haiku-20240307-v1:0",
            "claude-3-opus", "anthropic.claude-3-opus-20240229-v1:0",
            "claude-3-sonnet", "anthropic.claude-3-sonnet-20240229-v1:0",
            "claude-3-haiku", "anthropic.claude-3-haiku-20240307-v1:0");

    @Override
    public String getName() {
        return "bedrock";
    }

    @Override
    public List<String> getSupportedModels() {
        return List.of(
                "anthropic.claude-3-opus-20240229-v1:0",
                "anthropic.claude-3-sonnet-20240229-v1:0",
                "anthropic.claude-3-haiku-20240307-v1:0",
                "amazon.titan-text-express-v1",
                "ai21.j2-ultra-v1");
    }

    @Override
    public boolean isAvailable() {
        return accessKey != null && !accessKey.isBlank()
                && secretKey != null && !secretKey.isBlank();
    }

    @Override
    public ProviderMetadata getMetadata() {
        return ProviderMetadata.builder()
                .name("bedrock")
                .priority(4) // Lower priority (after OpenAI, Anthropic, Gemini)
                .enabled(isAvailable())
                .avgLatencyMs(700)
                .costPer1kInputTokens(new BigDecimal("0.015")) // Claude on Bedrock
                .costPer1kOutputTokens(new BigDecimal("0.075"))
                .maxTokens(200000)
                .supportsStreaming(true)
                .supportsFunctionCalling(true)
                .supportsVision(true)
                .maxRpm(3000)
                .healthCheckUrl("https://bedrock-runtime." + region + ".amazonaws.com")
                .build();
    }

    @Override
    public ChatResponse generate(ChatRequest request) {
        if (!isAvailable()) {
            throw new IllegalStateException("Bedrock provider is not configured. Set AWS credentials.");
        }

        log.debug("Bedrock provider generating completion for model: {}", request.getModel());

        String modelId = getEquivalentModel(request.getModel());
        return bedrockClient.generateCompletion(request, modelId);
    }

    @Override
    public Flux<ChatResponse> generateStream(ChatRequest request) {
        if (!isAvailable()) {
            throw new IllegalStateException("Bedrock provider is not configured. Set AWS credentials.");
        }

        log.debug("Bedrock provider generating streaming completion for model: {}", request.getModel());
        String modelId = getEquivalentModel(request.getModel());
        return bedrockClient.generateStream(request, modelId);
    }

    @Override
    public String getEquivalentModel(String requestedModel) {
        return MODEL_EQUIVALENTS.getOrDefault(requestedModel,
                "anthropic.claude-3-sonnet-20240229-v1:0");
    }
}
