package com.neurogate.router.upstream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neurogate.sentinel.model.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Client for interacting with AWS Bedrock API.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BedrockClient {

    private final ObjectMapper objectMapper;
    private final io.micrometer.core.instrument.MeterRegistry meterRegistry;

    @Value("${neurogate.bedrock.access-key:#{null}}")
    private String accessKey;

    @Value("${neurogate.bedrock.secret-key:#{null}}")
    private String secretKey;

    @Value("${neurogate.bedrock.region:us-east-1}")
    private String region;

    private BedrockRuntimeClient bedrockClient;

    /**
     * Initialize the Bedrock client.
     */
    @PostConstruct
    public void init() {
        if (accessKey != null && !accessKey.isBlank() && secretKey != null && !secretKey.isBlank()) {
            try {
                this.bedrockClient = BedrockRuntimeClient.builder()
                        .region(Region.of(region))
                        .credentialsProvider(StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKey, secretKey)))
                        .build();
                log.info("Bedrock client initialized successfully for region: {}", region);
            } catch (Exception e) {
                log.error("Failed to initialize Bedrock client", e);
            }
        } else {
            log.warn("Bedrock credentials not found, client not initialized");
        }
    }

    /**
     * Generate chat completion using Bedrock with resilience patterns.
     */
    @CircuitBreaker(name = "bedrock", fallbackMethod = "generateFallback")
    @Retry(name = "bedrock")
    public ChatResponse generateCompletion(ChatRequest request, String modelId) {
        if (bedrockClient == null) {
            throw new IllegalStateException("Bedrock client is not initialized. Check credentials.");
        }

        log.debug("Sending request to Bedrock: model={}", modelId);
        meterRegistry.counter("neurogate.upstream.requests", "provider", "bedrock").increment();

        try {
            // Build request based on model type
            Map<String, Object> bedrockRequest = buildBedrockRequest(modelId, request);
            String requestBody = objectMapper.writeValueAsString(bedrockRequest);

            // Invoke Bedrock model
            InvokeModelRequest invokeRequest = InvokeModelRequest.builder()
                    .modelId(modelId)
                    .body(SdkBytes.fromString(requestBody, StandardCharsets.UTF_8))
                    .build();

            InvokeModelResponse response = bedrockClient.invokeModel(invokeRequest);
            String responseBody = response.body().asString(StandardCharsets.UTF_8);

            ChatResponse chatResponse = convertToNeuroGateResponse(responseBody, request.getModel(), modelId);

            // Record cost (Estimate - Bedrock varies wildly, assume Claude Instant pricing
            // for now)
            if (chatResponse.getUsage() != null) {
                // ~$0.00163/1k in, $0.00551/1k out
                double cost = (chatResponse.getUsage().getPromptTokens() / 1000.0 * 0.002) +
                        (chatResponse.getUsage().getCompletionTokens() / 1000.0 * 0.006);
                chatResponse.setCostUsd(cost);
                meterRegistry.counter("neurogate.upstream.cost", "provider", "bedrock").increment(cost);
            }

            return chatResponse;

        } catch (Exception e) {
            log.error("Error calling Bedrock API", e);
            meterRegistry.counter("neurogate.upstream.errors", "provider", "bedrock").increment();
            throw new RuntimeException("Bedrock API call failed: " + e.getMessage(), e);
        }
    }

    /**
     * Build Bedrock-specific request format
     */
    private Map<String, Object> buildBedrockRequest(String modelId, ChatRequest request) {
        Map<String, Object> bedrockRequest = new HashMap<>();

        if (modelId.startsWith("anthropic")) {
            // Anthropic Claude format on Bedrock
            bedrockRequest.put("anthropic_version", "bedrock-2023-05-31");
            bedrockRequest.put("max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : 4096);
            bedrockRequest.put("messages", List.of(
                    Map.of(
                            "role", "user",
                            "content", request.getConcatenatedContent())));
            bedrockRequest.put("temperature", request.getTemperature() != null ? request.getTemperature() : 0.7);
        } else if (modelId.startsWith("amazon.titan")) {
            // Amazon Titan format
            bedrockRequest.put("inputText", request.getConcatenatedContent());
            bedrockRequest.put("textGenerationConfig", Map.of(
                    "maxTokenCount", request.getMaxTokens() != null ? request.getMaxTokens() : 4096,
                    "temperature", request.getTemperature() != null ? request.getTemperature() : 0.7,
                    "topP", request.getTopP() != null ? request.getTopP() : 0.9));
        } else if (modelId.startsWith("ai21")) {
            // AI21 Jurassic format
            bedrockRequest.put("prompt", request.getConcatenatedContent());
            bedrockRequest.put("maxTokens", request.getMaxTokens() != null ? request.getMaxTokens() : 4096);
            bedrockRequest.put("temperature", request.getTemperature() != null ? request.getTemperature() : 0.7);
        }

        return bedrockRequest;
    }

    /**
     * Convert Bedrock response to NeuroGate ChatResponse format
     */
    private ChatResponse convertToNeuroGateResponse(String responseBody, String requestedModel, String modelId) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String content;
            int inputTokens = 0;
            int outputTokens = 0;

            if (modelId.startsWith("anthropic")) {
                // Anthropic Claude format
                content = root.path("content").get(0).path("text").asText();
                inputTokens = root.path("usage").path("input_tokens").asInt(0);
                outputTokens = root.path("usage").path("output_tokens").asInt(0);
            } else if (modelId.startsWith("amazon.titan")) {
                // Amazon Titan format
                content = root.path("results").get(0).path("outputText").asText();
                inputTokens = root.path("inputTextTokenCount").asInt(0);
                outputTokens = root.path("results").get(0).path("tokenCount").asInt(0);
            } else {
                // AI21 Jurassic format
                content = root.path("completions").get(0).path("data").path("text").asText();
                inputTokens = root.path("prompt").path("tokens").size();
                outputTokens = root.path("completions").get(0).path("data").path("tokens").size();
            }

            return ChatResponse.builder()
                    .id("chatcmpl-bedrock-" + UUID.randomUUID().toString().substring(0, 8))
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
                                    .finishReason("stop")
                                    .build()))
                    .usage(Usage.builder()
                            .promptTokens(inputTokens)
                            .completionTokens(outputTokens)
                            .totalTokens(inputTokens + outputTokens)
                            .build())
                    .cacheHit(false)
                    .route("bedrock")
                    .build();

        } catch (Exception e) {
            log.error("Error parsing Bedrock response", e);
            throw new RuntimeException("Failed to parse Bedrock response: " + e.getMessage(), e);
        }
    }

    /**
     * Fallback method for circuit breaker.
     */
    private ChatResponse generateFallback(ChatRequest request, String modelId, Throwable throwable) {
        log.warn("Bedrock circuit breaker triggered, using fallback. Reason: {}", throwable.getMessage());

        return ChatResponse.builder()
                .id("chatcmpl-bedrock-fallback-" + UUID.randomUUID().toString().substring(0, 8))
                .object("chat.completion")
                .created(System.currentTimeMillis() / 1000)
                .model(request.getModel())
                .choices(List.of(
                        Choice.builder()
                                .index(0)
                                .message(Message.builder()
                                        .role("assistant")
                                        .content(
                                                "I apologize, but the AWS Bedrock service is temporarily unavailable. Please try again in a moment.")
                                        .build())
                                .finishReason("error")
                                .build()))
                .usage(Usage.builder()
                        .promptTokens(0)
                        .completionTokens(0)
                        .totalTokens(0)
                        .build())
                .cacheHit(false)
                .route("bedrock-fallback")
                .error("Bedrock service temporarily unavailable: " + throwable.getMessage())
                .build();
    }

    /**
     * Generate streaming chat completion using Bedrock
     */
    @CircuitBreaker(name = "bedrock-stream", fallbackMethod = "generateStreamFallback")
    @Retry(name = "bedrock-stream")
    public reactor.core.publisher.Flux<ChatResponse> generateStream(ChatRequest request, String modelId) {
        if (bedrockClient == null) {
            throw new IllegalStateException("Bedrock client is not initialized. Check credentials.");
        }

        log.debug("Sending streaming request to Bedrock: model={}", modelId);

        return reactor.core.publisher.Flux
                .error(new UnsupportedOperationException("Bedrock streaming not yet implemented"));
    }

    private ChatResponse convertToNeuroGateStreamResponse(String chunkJson, String requestedModel, String modelId) {
        try {
            JsonNode root = objectMapper.readTree(chunkJson);
            String content = "";
            String finishReason = null;

            if (modelId.startsWith("anthropic")) {
                // Anthropic stream chunk format on Bedrock
                // Usually { "completion": "...", "stop_reason": ... } or { "delta": { ... } }
                // depending on version
                // Messages API: { "type": "content_block_delta", "delta": { "text": "..." } }

                String type = root.path("type").asText();
                if ("content_block_delta".equals(type)) {
                    content = root.path("delta").path("text").asText("");
                } else if ("message_delta".equals(type)) {
                    finishReason = root.path("delta").path("stop_reason").asText(null);
                }
            } else if (modelId.startsWith("amazon.titan")) {
                content = root.path("outputText").asText("");
                // Titan might send full text or chunks? Assuming chunks for stream
            } else {
                // Fallback for others
                // Adjust based on specific model response structure
            }

            return ChatResponse.builder()
                    .id("chatcmpl-bedrock-" + UUID.randomUUID().toString().substring(0, 8))
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
                    .route("bedrock")
                    .build();

        } catch (Exception e) {
            // Swallow parsing errors to keep stream alive
            return ChatResponse.builder().id("error").choices(List.of()).build();
        }
    }

    private reactor.core.publisher.Flux<ChatResponse> generateStreamFallback(ChatRequest request, String modelId,
            Throwable throwable) {
        log.warn("Bedrock stream circuit breaker triggered, using fallback. Reason: {}", throwable.getMessage());
        return reactor.core.publisher.Flux.just(
                ChatResponse.builder()
                        .id("chatcmpl-bedrock-fallback-" + UUID.randomUUID().toString().substring(0, 8))
                        .object("chat.completion.chunk")
                        .created(System.currentTimeMillis() / 1000)
                        .model(request.getModel())
                        .choices(List.of(
                                Choice.builder()
                                        .index(0)
                                        .delta(Message.builder()
                                                .role("assistant")
                                                .content(
                                                        "I apologize, but the AWS Bedrock service is temporarily unavailable.")
                                                .build())
                                        .finishReason("stop")
                                        .build()))
                        .build());
    }
}
