package com.neurogate.sentinel;

import com.neurogate.exception.NeuroGateException;
import com.neurogate.pulse.PulseEventPublisher;
import com.neurogate.pulse.model.PulseEvent;
import com.neurogate.router.provider.MultiProviderRouter;
import com.neurogate.sentinel.model.ChatRequest;
import com.neurogate.sentinel.model.ChatResponse;
import com.neurogate.sentinel.model.Message;
import com.neurogate.vault.PiiRestorerFactory;
import com.neurogate.vault.PiiSanitizationService;
import com.neurogate.vault.StreamingPiiRestorer;
import com.neurogate.vault.model.SanitizedPrompt;
import com.neurogate.vault.neuroguard.ActiveDefenseService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SentinelService {

    private final MultiProviderRouter multiProviderRouter;
    private final PiiSanitizationService piiSanitizationService;
    private final PiiRestorerFactory piiRestorerFactory;
    private final ActiveDefenseService activeDefenseService;
    private final PulseEventPublisher pulseEventPublisher;
    private final com.neurogate.agent.AgentLoopDetector agentLoopDetector;
    private final com.neurogate.validation.StructuredOutputService structuredOutputService;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;

    @SuppressWarnings("unused")
    public ChatResponse processRequest(ChatRequest request) {
        String requestId = UUID.randomUUID().toString();

        publishReceivedEvent(requestId, request);
        enrichRequestLogging(request);
        validateRequest(request);
        agentLoopDetector.validateRequest(request);

        long startTime = System.currentTimeMillis();

        try {
            ChatResponse response;

            // Use structured output service if json_schema format is requested
            if (requiresStructuredOutputValidation(request)) {
                log.debug("Using structured output validation for json_schema response format");
                response = structuredOutputService.generateWithValidation(request);
            } else {
                response = multiProviderRouter.route(request);
            }

            long latency = System.currentTimeMillis() - startTime;
            response.setLatencyMs(latency);

            publishResponseEvent(requestId, request, response, latency);

            log.info("Request completed in {}ms, cache hit: {}, route: {}, validation: {}",
                    latency, response.getCacheHit(), response.getRoute(),
                    response.getValidation() != null ? response.getValidation().isSchemaValid() : "N/A");

            return response;

        } catch (Exception e) {
            publishErrorEvent(requestId, request, e);
            log.error("Error processing chat request", e);
            if (e instanceof NeuroGateException neuroGateException) {
                throw neuroGateException;
            }
            if (e instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException("Failed to process chat request: " + e.getMessage(), e);
        }
    }

    /**
     * Check if the request requires structured output validation.
     */
    private boolean requiresStructuredOutputValidation(ChatRequest request) {
        if (request.getResponseFormat() == null) return false;
        return "json_schema".equals(request.getResponseFormat().getType()) &&
                request.getResponseFormat().getJsonSchema() != null;
    }

    /**
     * Process a streaming chat request with PII handling
     */
    public Flux<ChatResponse> processStreamRequest(ChatRequest request) {
        log.info("Received STREAMING chat request for model: {}, messages: {}",
                request.getModel(), request.getMessages().size());

        validateRequest(request);
        agentLoopDetector.validateRequest(request);

        String originalPrompt = request.getConcatenatedContent();
        SanitizedPrompt sanitizedPrompt = piiSanitizationService.sanitize(originalPrompt);

        if (sanitizedPrompt.isContainsPii()) {
            log.info("PII detected and sanitized for streaming: {} entities",
                    sanitizedPrompt.getDetectedEntities().size());
        }

        ChatRequest sanitizedRequest = createSanitizedRequest(request, sanitizedPrompt.getSanitizedText());

        // Thread-safe restorer for this stream
        StreamingPiiRestorer restorer = piiRestorerFactory.createRestorer();
        CircuitBreaker streamCircuitBreaker = circuitBreakerRegistry.circuitBreaker("streaming");
        Retry streamRetry = retryRegistry.retry("streaming");

        return Flux.concat(
                multiProviderRouter.routeStream(sanitizedRequest)
                        .transformDeferred(CircuitBreakerOperator.of(streamCircuitBreaker))
                        .transformDeferred(RetryOperator.of(streamRetry))
                        .map(chunk -> {
                            if (sanitizedPrompt.isContainsPii() && chunk.getChoices() != null) {
                                chunk.getChoices().forEach(choice -> {
                                    if (choice.getMessage() != null && choice.getMessage().getContent() != null) {
                                        String content = choice.getMessage().getStrContent();
                                        String restoredContent = restorer.processChunk(content);
                                        choice.getMessage().setContent(restoredContent);
                                    }
                                });
                            }
                            return chunk;
                        }),
                reactor.core.publisher.Mono.fromSupplier(() -> {
                    String remaining = restorer.flush();
                    if (remaining != null && !remaining.isEmpty()) {
                        ChatResponse finalChunk = new ChatResponse();
                        finalChunk.setId("pii-flush-" + System.currentTimeMillis());
                        finalChunk.setModel(request.getModel());
                        finalChunk.setCreated(System.currentTimeMillis() / 1000);

                        com.neurogate.sentinel.model.Choice choice = new com.neurogate.sentinel.model.Choice();
                        choice.setIndex(0);

                        com.neurogate.sentinel.model.Message message = new com.neurogate.sentinel.model.Message();
                        message.setRole("assistant");
                        message.setContent(remaining);

                        choice.setMessage(message);
                        finalChunk.setChoices(java.util.List.of(choice));
                        return finalChunk;
                    }
                    return null;
                }).flatMap(response -> response != null ? reactor.core.publisher.Mono.just(response)
                        : reactor.core.publisher.Mono.empty()))
                .doOnError(error -> {
                    log.error("Error in streaming response", error);
                });
    }

    private void validateRequest(ChatRequest request) {
        if (request.getMessages() != null) {
            request.getMessages().stream()
                    .filter(m -> "user".equals(m.getRole()))
                    .reduce((first, second) -> second)
                    .ifPresent(msg -> {
                        String original = msg.getStrContent();
                        String sanitized = activeDefenseService.validatePrompt(original);
                        if (!original.equals(sanitized)) {
                            log.info("Prompt sanitized by Active Defense: {} -> {}", original, sanitized);
                            msg.setContent(sanitized);
                        }
                    });
        }
    }

    private ChatRequest createSanitizedRequest(ChatRequest original, String sanitizedText) {
        List<Message> copiedMessages = deepCopyMessages(original.getMessages());
        if (!copiedMessages.isEmpty()) {
            int lastIndex = copiedMessages.size() - 1;
            copiedMessages.get(lastIndex).setContent(sanitizedText);
        }

        return original.toBuilder()
                .messages(copiedMessages)
                .build();
    }

    private List<Message> deepCopyMessages(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        List<Message> copies = new ArrayList<>(messages.size());
        for (Message message : messages) {
            copies.add(copyMessage(message));
        }
        return copies;
    }

    private Message copyMessage(Message original) {
        return Message.builder()
                .role(original.getRole())
                .name(original.getName())
                .content(deepCopyContent(original.getContent()))
                .build();
    }

    private Object deepCopyContent(Object content) {
        if (content instanceof List<?> list) {
            List<Object> copied = new ArrayList<>(list.size());
            for (Object item : list) {
                copied.add(deepCopyContent(item));
            }
            return copied;
        }
        if (content instanceof Map<?, ?> map) {
            Map<String, Object> copied = new HashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                copied.put(String.valueOf(entry.getKey()), deepCopyContent(entry.getValue()));
            }
            return copied;
        }
        return content;
    }

    private void enrichRequestLogging(ChatRequest request) {
        if (request.getTraceId() == null) {
            request.setTraceId(org.slf4j.MDC.get("traceId"));
        }
        if (request.getSessionId() == null) {
            request.setSessionId(org.slf4j.MDC.get("sessionId"));
        }
        log.info("Received chat request for model: {}, messages: {}",
                request.getModel(), request.getMessages().size());
    }

    private void publishReceivedEvent(String requestId, ChatRequest request) {
        pulseEventPublisher.publish(PulseEvent.builder()
                .id(requestId)
                .type(PulseEvent.EventType.REQUEST_RECEIVED)
                .timestamp(Instant.now())
                .model(request.getModel())
                .userId(request.getUser())
                .message("Request received")
                .build());
    }

    private void publishResponseEvent(String requestId, ChatRequest request, ChatResponse response, long latency) {
        pulseEventPublisher.publish(PulseEvent.builder()
                .id(requestId)
                .type(PulseEvent.EventType.RESPONSE_SENT)
                .timestamp(Instant.now())
                .provider(response.getRoute())
                .model(request.getModel())
                .userId(request.getUser())
                .latencyMs(latency)
                .tokenCount(response.getUsage() != null ? response.getUsage().getTotalTokens() : null)
                .costUsd(response.getCostUsd())
                .cacheHit(response.getCacheHit())
                .message("Response sent")
                .build());
    }

    private void publishErrorEvent(String requestId, ChatRequest request, Exception e) {
        pulseEventPublisher.publish(PulseEvent.builder()
                .id(requestId)
                .type(PulseEvent.EventType.ERROR)
                .timestamp(Instant.now())
                .model(request.getModel())
                .error(e.getMessage())
                .build());
    }

}
