package com.neurogate.flywheel;

import com.neurogate.agentops.model.Trace;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Filters traces to ensure only high-quality interaction data enters the
 * flywheel.
 */
@Slf4j
@Component
public class QualityFilter {

    private static final long LATENCY_THRESHOLD_MS = 2000;

    /**
     * Determines if a trace is "Golden" (suitable for fine-tuning).
     */
    public boolean isGolden(Trace trace) {
        if (trace == null || trace.getError() != null) {
            return false;
        }

        // 1. Latency Check: Must be fast enough (implies efficient reasoning)
        if (trace.getDurationMs() != null && trace.getDurationMs() > LATENCY_THRESHOLD_MS) {
            return false;
        }

        // 2. Feedback Check (if available) - simulate a "user_feedback" attribute
        // In a real system, we'd check feedback database
        // boolean positiveFeedback =
        // feedbackService.hasPositiveFeedback(trace.getId());

        // 3. Heuristic: Did it use tools successfully?
        boolean hasToolUse = trace.getSpans() != null && trace.getSpans().stream()
                .anyMatch(s -> "TOOL_CALL".equals(s.getType().name()) && "COMPLETED".equals(s.getStatus().name()));

        return hasToolUse;
    }
}
