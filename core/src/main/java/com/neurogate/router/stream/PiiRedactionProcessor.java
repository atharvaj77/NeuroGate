package com.neurogate.router.stream;

import com.neurogate.sentinel.model.ChatResponse;
import com.neurogate.sentinel.model.Choice;
import com.neurogate.sentinel.model.Message;
import com.neurogate.vault.PiiSanitizationService;
import com.neurogate.vault.StreamingPiiRestorer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Processes streaming responses to restore PII tokens.
 * Handles chunked tokens like "<EMAIL_1>" that may span multiple chunks.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PiiRedactionProcessor implements StreamProcessor {

    private final PiiSanitizationService piiSanitizationService;
    private final ThreadLocal<StreamingPiiRestorer> restorerHolder = new ThreadLocal<>();

    @Override
    public Flux<ChatResponse> process(Flux<ChatResponse> stream) {
        StreamingPiiRestorer restorer = new StreamingPiiRestorer(piiSanitizationService);
        AtomicReference<ChatResponse> lastResponseRef = new AtomicReference<>();

        return stream
                .map(response -> processResponse(response, restorer, lastResponseRef))
                .concatWith(Flux.defer(() -> createFlushPacket(restorer, lastResponseRef.get())));
    }

    private ChatResponse processResponse(
            ChatResponse response,
            StreamingPiiRestorer restorer,
            AtomicReference<ChatResponse> lastResponseRef) {

        lastResponseRef.set(response);

        if (response.getChoices() == null || response.getChoices().isEmpty()) {
            return response;
        }

        Choice choice = response.getChoices().get(0);
        if (choice.getDelta() == null) {
            return response;
        }

        String content = choice.getDelta().getStrContent();
        if (content == null) {
            return response;
        }

        String restored = restorer.processChunk(content);
        if (!restored.equals(content) && !restored.isEmpty()) {
            response.setPiiDetected(1);
        }

        choice.getDelta().setContent(restored);
        return response;
    }

    private Flux<ChatResponse> createFlushPacket(StreamingPiiRestorer restorer, ChatResponse template) {
        String remainder = restorer.flush();

        if (remainder == null || remainder.isEmpty() || template == null) {
            return Flux.empty();
        }

        ChatResponse flushPacket = ChatResponse.builder()
                .id(template.getId())
                .object(template.getObject())
                .created(template.getCreated())
                .model(template.getModel())
                .traceId(template.getTraceId())
                .sessionId(template.getSessionId())
                .choices(List.of(Choice.builder()
                        .index(0)
                        .delta(Message.builder()
                                .role("assistant")
                                .content(remainder)
                                .build())
                        .build()))
                .build();

        return Flux.just(flushPacket);
    }

    @Override
    public int getPriority() {
        return 10; // Run first
    }

    @Override
    public String getName() {
        return "pii-redaction";
    }
}
