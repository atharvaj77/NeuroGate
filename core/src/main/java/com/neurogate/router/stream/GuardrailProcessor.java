package com.neurogate.router.stream;

import com.neurogate.sentinel.model.ChatResponse;
import com.neurogate.sentinel.model.Choice;
import com.neurogate.vault.streaming.StreamingGuardrail;
import com.neurogate.vault.streaming.StreamingResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Processes streaming responses for content safety guardrails.
 * Can abort streams that contain harmful content.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GuardrailProcessor implements StreamProcessor {

    private final StreamingGuardrail streamingGuardrail;

    @Override
    public Flux<ChatResponse> process(Flux<ChatResponse> stream) {
        if (!isEnabled()) {
            return stream;
        }

        streamingGuardrail.reset();
        AtomicBoolean aborted = new AtomicBoolean(false);

        return stream
                .map(response -> processResponse(response, aborted))
                .takeUntil(response -> isAbortSignal(response))
                .doFinally(signal -> streamingGuardrail.reset());
    }

    private ChatResponse processResponse(ChatResponse response, AtomicBoolean aborted) {
        if (aborted.get()) {
            return response;
        }

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

        StreamingResult result = streamingGuardrail.processToken(content);

        if (!result.isShouldContinue()) {
            aborted.set(true);
            choice.getDelta().setContent(
                    "\n\n[Stream terminated: " + result.getAbortReason() + "]");
            choice.setFinishReason("content_filter");
            log.warn("🛡️ Stream guardrail triggered: {}", result.getViolationCategory());
        } else if (result.getToken() != null) {
            choice.getDelta().setContent(result.getToken());
        }

        return response;
    }

    private boolean isAbortSignal(ChatResponse response) {
        if (response.getChoices() == null || response.getChoices().isEmpty()) {
            return false;
        }
        String finishReason = response.getChoices().get(0).getFinishReason();
        return "content_filter".equals(finishReason);
    }

    @Override
    public int getPriority() {
        return 20; // Run after PII redaction
    }

    @Override
    public String getName() {
        return "guardrail";
    }

    @Override
    public boolean isEnabled() {
        return streamingGuardrail != null && streamingGuardrail.isEnabled();
    }

    @Override
    public void reset() {
        if (streamingGuardrail != null) {
            streamingGuardrail.reset();
        }
    }
}
