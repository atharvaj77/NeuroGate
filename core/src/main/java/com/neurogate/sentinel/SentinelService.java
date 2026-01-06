package com.neurogate.sentinel;

import com.neurogate.pulse.PulseEventPublisher;
import com.neurogate.pulse.model.PulseEvent;
import com.neurogate.router.provider.MultiProviderRouter;
import com.neurogate.sentinel.model.ChatRequest;
import com.neurogate.sentinel.model.ChatResponse;
import com.neurogate.vault.PiiRestorerFactory;
import com.neurogate.vault.PiiSanitizationService;
import com.neurogate.vault.StreamingPiiRestorer;
import com.neurogate.vault.model.SanitizedPrompt;
import com.neurogate.vault.neuroguard.ActiveDefenseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Instant;
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

    @SuppressWarnings("unused")
    public ChatResponse processRequest(ChatRequest request) {
        String requestId = UUID.randomUUID().toString();

        publishReceivedEvent(requestId, request);
        enrichRequestLogging(request);
        validateRequest(request);
        agentLoopDetector.validateRequest(request);

        long startTime = System.currentTimeMillis();

        try {
            ChatResponse response = multiProviderRouter.route(request);

            long latency = System.currentTimeMillis() - startTime;
            response.setLatencyMs(latency);

            publishResponseEvent(requestId, request, response, latency);

            log.info("Request completed in {}ms, cache hit: {}, route: {}",
                    latency, response.getCacheHit(), response.getRoute());

            return response;

        } catch (Exception e) {
            publishErrorEvent(requestId, request, e);
            log.error("Error processing chat request", e);
            throw new RuntimeException("Failed to process chat request: " + e.getMessage(), e);
        }
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

        return Flux.concat(
                multiProviderRouter.routeStream(sanitizedRequest)
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
                    .ifPresent(msg -> activeDefenseService.validatePrompt(msg.getStrContent()));
        }
    }

    private ChatRequest createSanitizedRequest(ChatRequest original, String sanitizedText) {
        ChatRequest sanitized = ChatRequest.builder()
                .model(original.getModel())
                .messages(original.getMessages())
                .temperature(original.getTemperature())
                .maxTokens(original.getMaxTokens())
                .topP(original.getTopP())
                .frequencyPenalty(original.getFrequencyPenalty())
                .presencePenalty(original.getPresencePenalty())
                .stop(original.getStop())
                .stream(original.getStream())
                .user(original.getUser())
                .build();

        if (original.getCanaryWeight() != null) {
            sanitized.setCanaryWeight(original.getCanaryWeight());
        }

        if (!original.getMessages().isEmpty()) {
            int lastIndex = original.getMessages().size() - 1;
            original.getMessages().get(lastIndex).setContent(sanitizedText);
        }

        return sanitized;
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
