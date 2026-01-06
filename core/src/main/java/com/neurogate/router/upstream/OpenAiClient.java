package com.neurogate.router.upstream;

import com.neurogate.sentinel.model.*;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Client for interacting with OpenAI API with resilience patterns.
 * Uses Spring AI for the actual API calls.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiClient {

        private final ChatModel chatModel;
        private final io.micrometer.core.instrument.MeterRegistry meterRegistry;

        /**
         * Generate chat completion using OpenAI with resilience patterns.
         */
        @CircuitBreaker(name = "openai", fallbackMethod = "generateCompletionFallback")
        @Retry(name = "openai")
        @RateLimiter(name = "openai")
        @Bulkhead(name = "openai")
        public ChatResponse generateCompletion(ChatRequest request) {
                log.debug("Sending request to OpenAI: model={}", request.getModel());
                meterRegistry.counter("neurogate.upstream.requests", "provider", "openai").increment();

                try {
                        // Convert NeuroGate messages to Spring AI messages
                        List<org.springframework.ai.chat.messages.Message> springAiMessages = request.getMessages()
                                        .stream()
                                        .map(this::toSpringAiMessage)
                                        .collect(Collectors.toList());

                        // Build options
                        OpenAiChatOptions options = OpenAiChatOptions.builder()
                                        .withModel(request.getModel())
                                        .withTemperature(request.getTemperature() != null ? request.getTemperature()
                                                        : 0.7)
                                        .withMaxTokens(request.getMaxTokens() != null ? request.getMaxTokens() : 1000)
                                        .build();

                        // Call OpenAI
                        Prompt prompt = new Prompt(springAiMessages, options);
                        org.springframework.ai.chat.model.ChatResponse springAiResponse = chatModel.call(prompt);

                        // Convert Spring AI response to NeuroGate format
                        ChatResponse response = convertToNeuroGateResponse(springAiResponse, request.getModel());

                        // Record cost metric
                        if (response.getCostUsd() != null) {
                                meterRegistry.counter("neurogate.upstream.cost", "provider", "openai")
                                                .increment(response.getCostUsd());
                        }

                        return response;

                } catch (Exception e) {
                        log.error("Error calling OpenAI API", e);
                        meterRegistry.counter("neurogate.upstream.errors", "provider", "openai").increment();
                        throw new RuntimeException("OpenAI API call failed: " + e.getMessage(), e);
                }
        }

        private org.springframework.ai.chat.messages.Message toSpringAiMessage(
                        com.neurogate.sentinel.model.Message msg) {
                String role = msg.getRole() != null ? msg.getRole() : "user";

                if ("system".equalsIgnoreCase(role)) {
                        return new org.springframework.ai.chat.messages.SystemMessage(msg.getStrContent());
                } else if ("assistant".equalsIgnoreCase(role)) {
                        return new org.springframework.ai.chat.messages.AssistantMessage(msg.getStrContent());
                } else {
                        // User message (handles multimodal)
                        if (msg.getContent() instanceof String) {
                                return new org.springframework.ai.chat.messages.UserMessage((String) msg.getContent());
                        } else if (msg.getContent() instanceof List) {
                                // Handle list content (text + items)
                                StringBuilder textBuilder = new StringBuilder();
                                List<org.springframework.ai.model.Media> mediaList = new java.util.ArrayList<>();

                                List<?> parts = (List<?>) msg.getContent();
                                for (Object part : parts) {
                                        if (part instanceof java.util.Map) {
                                                java.util.Map<?, ?> map = (java.util.Map<?, ?>) part;
                                                String type = (String) map.get("type");
                                                if ("text".equals(type)) {
                                                        textBuilder.append(map.get("text")).append(" ");
                                                } else if ("image_url".equals(type)) {
                                                        java.util.Map<?, ?> imageUrl = (java.util.Map<?, ?>) map
                                                                        .get("image_url");
                                                        String url = (String) imageUrl.get("url");
                                                        try {
                                                                // Basic assumption: URL is accessible. Spring AI Media
                                                                // handles URL.
                                                                // For production, might need MimeType detection.
                                                                // defaulting to image/jpeg
                                                                mediaList.add(new org.springframework.ai.model.Media(
                                                                                org.springframework.util.MimeTypeUtils.IMAGE_JPEG,
                                                                                new java.net.URL(url)));
                                                        } catch (Exception e) {
                                                                log.warn("Invalid image URL in request: {}", url);
                                                        }
                                                }
                                        }
                                }
                                return new org.springframework.ai.chat.messages.UserMessage(
                                                textBuilder.toString().trim(), mediaList);
                        }
                        // Fallback
                        return new org.springframework.ai.chat.messages.UserMessage(msg.getStrContent());
                }
        }

        /**
         * Fallback method for circuit breaker.
         */
        private ChatResponse generateCompletionFallback(ChatRequest request, Throwable throwable) {
                log.warn("OpenAI circuit breaker triggered, using fallback. Reason: {}",
                                throwable.getMessage());

                return ChatResponse.builder()
                                .id("chatcmpl-fallback-" + UUID.randomUUID().toString().substring(0, 8))
                                .object("chat.completion")
                                .created(System.currentTimeMillis() / 1000)
                                .model(request.getModel())
                                .choices(List.of(
                                                Choice.builder()
                                                                .index(0)
                                                                .message(Message.builder()
                                                                                .role("assistant")
                                                                                .content("I apologize, but I'm experiencing temporary difficulties connecting to the AI service. Please try again in a moment.")
                                                                                .build())
                                                                .finishReason("error")
                                                                .build()))
                                .usage(Usage.builder()
                                                .promptTokens(0)
                                                .completionTokens(0)
                                                .totalTokens(0)
                                                .build())
                                .cacheHit(false)
                                .route("fallback")
                                .error("Service temporarily unavailable due to: " + throwable.getMessage())
                                .build();
        }

        /**
         * Convert Spring AI response to NeuroGate ChatResponse format
         */
        private ChatResponse convertToNeuroGateResponse(
                        org.springframework.ai.chat.model.ChatResponse springAiResponse,
                        String model) {

                // Extract content from Spring AI response
                String content = springAiResponse.getResult().getOutput().getContent();

                // Extract Usage
                org.springframework.ai.chat.metadata.Usage springUsage = springAiResponse.getMetadata().getUsage();
                int promptTokens = springUsage != null ? springUsage.getPromptTokens().intValue() : 0;
                int completionTokens = springUsage != null ? springUsage.getGenerationTokens().intValue() : 0;
                int totalTokens = promptTokens + completionTokens;

                // Estimate Cost (Simple heuristic for now: Avg $0.002 / 1k tokens)
                // In production, use a PricingService based on model name
                double cost = (totalTokens / 1000.0) * 0.002;

                // Build NeuroGate response
                return ChatResponse.builder()
                                .id("chatcmpl-" + UUID.randomUUID().toString().substring(0, 8))
                                .object("chat.completion")
                                .created(System.currentTimeMillis() / 1000)
                                .model(model)
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
                                                .promptTokens(promptTokens)
                                                .completionTokens(completionTokens)
                                                .totalTokens(totalTokens)
                                                .build())
                                .costUsd(cost)
                                .cacheHit(false)
                                .route("openai")
                                .build();
        }

        /**
         * Generate streaming chat completion using OpenAI.
         */
        @CircuitBreaker(name = "openai-stream", fallbackMethod = "generateStreamFallback")
        @Retry(name = "openai-stream")
        @RateLimiter(name = "openai")
        @Bulkhead(name = "openai")
        public reactor.core.publisher.Flux<ChatResponse> generateStream(ChatRequest request) {
                log.debug("Sending streaming request to OpenAI: model={}", request.getModel());

                try {
                        List<org.springframework.ai.chat.messages.Message> springAiMessages = request.getMessages()
                                        .stream()
                                        .map(this::toSpringAiMessage)
                                        .collect(Collectors.toList());

                        OpenAiChatOptions options = OpenAiChatOptions.builder()
                                        .withModel(request.getModel())
                                        .withTemperature(request.getTemperature() != null ? request.getTemperature()
                                                        : 0.7)
                                        .withMaxTokens(request.getMaxTokens() != null ? request.getMaxTokens() : 1000)
                                        .build();

                        Prompt prompt = new Prompt(springAiMessages, options);

                        return chatModel.stream(prompt)
                                        .map(springAiResponse -> convertToNeuroGateStreamResponse(springAiResponse,
                                                        request.getModel()));

                } catch (Exception e) {
                        log.error("Error calling OpenAI stream API", e);
                        throw new RuntimeException("OpenAI stream API call failed: " + e.getMessage(), e);
                }
        }

        private ChatResponse convertToNeuroGateStreamResponse(
                        org.springframework.ai.chat.model.ChatResponse springAiResponse,
                        String model) {

                String content = springAiResponse.getResult().getOutput().getContent();
                // In streaming, content is partial (delta)

                return ChatResponse.builder()
                                .id("chatcmpl-" + UUID.randomUUID().toString().substring(0, 8)) // ID might change per
                                                                                                // chunk
                                                                                                // but usually static in
                                                                                                // stream. Spring AI
                                                                                                // re-emits.
                                .object("chat.completion.chunk")
                                .created(System.currentTimeMillis() / 1000)
                                .model(model)
                                .choices(List.of(
                                                Choice.builder()
                                                                .index(0)
                                                                .delta(Message.builder() // Use delta for stream
                                                                                .role("assistant") // Role is often only
                                                                                                   // in
                                                                                                   // first chunk but
                                                                                                   // safe
                                                                                                   // to repeat
                                                                                .content(content != null ? content : "")
                                                                                .build())
                                                                .finishReason(null) // Finish reason is usually at the
                                                                                    // end
                                                                .build()))
                                .usage(null) // Usage is typically null in chunks or only in final chunk
                                .cacheHit(false)
                                .route("openai")
                                .build();
        }

        private reactor.core.publisher.Flux<ChatResponse> generateStreamFallback(ChatRequest request,
                        Throwable throwable) {
                log.warn("OpenAI stream circuit breaker triggered, using fallback. Reason: {}", throwable.getMessage());
                return reactor.core.publisher.Flux.just(
                                ChatResponse.builder()
                                                .id("chatcmpl-fallback-" + UUID.randomUUID().toString().substring(0, 8))
                                                .object("chat.completion.chunk")
                                                .created(System.currentTimeMillis() / 1000)
                                                .model(request.getModel())
                                                .choices(List.of(
                                                                Choice.builder()
                                                                                .index(0)
                                                                                .delta(Message.builder()
                                                                                                .role("assistant")
                                                                                                .content("I apologize, but I'm experiencing temporary difficulties connecting to the AI service.")
                                                                                                .build())
                                                                                .finishReason("stop")
                                                                                .build()))
                                                .build());
        }
}
