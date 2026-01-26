package com.neurogate.vault.streaming;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;

/**
 * Real-time content moderation for streaming LLM responses.
 * Processes tokens as they arrive and can abort streams early.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StreamingGuardrail {

    private final StreamingGuardrailConfig config;

    // Thread-local state for each stream
    private static final ThreadLocal<GuardrailState> STATE = ThreadLocal.withInitial(GuardrailState::new);

    /**
     * Process a single token from the stream.
     *
     * @param token The token to process
     * @return Result indicating whether to continue and any modifications
     */
    public StreamingResult processToken(String token) {
        if (!config.isEnabled()) {
            return StreamingResult.ok(token, 0);
        }

        GuardrailState state = STATE.get();

        // Check if already aborted
        if (state.isAborted()) {
            return StreamingResult.alreadyAborted(state.getAbortReason());
        }

        // Add token to buffer
        state.getBuffer().append(token);

        // Trim buffer to max size (keep most recent)
        if (state.getBuffer().length() > config.getBufferSize()) {
            int excess = state.getBuffer().length() - config.getBufferSize();
            state.getBuffer().delete(0, excess);
        }

        String bufferContent = state.getBuffer().toString();

        // Check patterns against buffer
        for (ToxicityPattern pattern : config.getCompiledPatterns()) {
            Matcher matcher = pattern.getPattern().matcher(bufferContent);

            if (matcher.find()) {
                return handlePatternMatch(state, pattern, token);
            }
        }

        // Check if toxicity threshold exceeded
        if (state.getToxicityScore() > config.getToxicityThreshold()) {
            state.setAborted(true);
            state.setAbortReason("Cumulative toxicity threshold exceeded");

            log.warn("🛡️ Stream aborted: toxicity {} > threshold {}",
                    state.getToxicityScore(), config.getToxicityThreshold());

            return StreamingResult.abort(
                    "Content policy: cumulative toxicity exceeded safe threshold",
                    "TOXICITY_THRESHOLD"
            );
        }

        // Check max warnings
        if (state.getWarningCount() > config.getMaxWarnings()) {
            state.setAborted(true);
            state.setAbortReason("Too many content warnings");

            log.warn("🛡️ Stream aborted: {} warnings exceeded max {}",
                    state.getWarningCount(), config.getMaxWarnings());

            return StreamingResult.abort(
                    "Content policy: too many warnings triggered",
                    "MAX_WARNINGS"
            );
        }

        return StreamingResult.ok(token, state.getToxicityScore());
    }

    /**
     * Handle a pattern match based on its severity and action.
     */
    private StreamingResult handlePatternMatch(
            GuardrailState state,
            ToxicityPattern pattern,
            String token
    ) {
        log.debug("🛡️ Pattern matched: {} (severity: {}, action: {})",
                pattern.getCategory(), pattern.getSeverity(), pattern.getAction());

        // Add toxicity points
        state.setToxicityScore(state.getToxicityScore() + pattern.getToxicityPoints());

        switch (pattern.getAction()) {
            case ABORT:
                state.setAborted(true);
                state.setAbortReason(pattern.getDescription());

                log.warn("🛡️ Stream ABORTED: {} - {}",
                        pattern.getCategory(), pattern.getDescription());

                return StreamingResult.abort(
                        "Content policy violation: " + pattern.getDescription(),
                        pattern.getCategory()
                );

            case FILTER:
                // Replace the token with [FILTERED]
                state.incrementWarningCount();
                log.info("🛡️ Content filtered: {}", pattern.getCategory());

                return StreamingResult.builder()
                        .token("[FILTERED]")
                        .shouldContinue(true)
                        .toxicityLevel(state.getToxicityScore())
                        .warningCount(state.getWarningCount())
                        .violationCategory(pattern.getCategory())
                        .build();

            case WARN:
                state.incrementWarningCount();
                log.info("🛡️ Warning triggered: {} (count: {}, toxicity: {})",
                        pattern.getCategory(), state.getWarningCount(), state.getToxicityScore());

                return StreamingResult.warn(token, state.getToxicityScore(), state.getWarningCount());

            case LOG:
            default:
                log.debug("🛡️ Pattern logged: {}", pattern.getCategory());
                return StreamingResult.ok(token, state.getToxicityScore());
        }
    }

    /**
     * Reset state for a new stream.
     * MUST be called at the start of each new stream.
     */
    public void reset() {
        STATE.remove();
        log.debug("🛡️ Guardrail state reset for new stream");
    }

    /**
     * Get current state (for debugging/monitoring).
     */
    public GuardrailState getCurrentState() {
        return STATE.get();
    }

    /**
     * Check if guardrails are enabled.
     */
    public boolean isEnabled() {
        return config.isEnabled();
    }

    /**
     * Internal state for tracking stream analysis.
     */
    @lombok.Data
    public static class GuardrailState {
        private StringBuilder buffer = new StringBuilder();
        private int toxicityScore = 0;
        private int warningCount = 0;
        private boolean aborted = false;
        private String abortReason;

        public void incrementWarningCount() {
            this.warningCount++;
        }
    }
}