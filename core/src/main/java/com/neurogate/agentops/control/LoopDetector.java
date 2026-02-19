package com.neurogate.agentops.control;

import com.neurogate.agentops.model.Trace;
import com.neurogate.agentops.model.Span;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Detects infinite loops in agent execution.
 */
@Slf4j
@Component
public class LoopDetector {

    private static final int LOOP_THRESHOLD = 3;

    /**
     * Detects if the recent trace constitutes a loop.
     * 
     * @param history List of recent traces for the session (ordered newest to
     *                oldest)
     * @return true if loop detected
     */
    public boolean isLooping(List<Trace> history) {
        // Need at least threshold + 1 traces to detect a repeating pattern of length 1
        if (history == null || history.size() < LOOP_THRESHOLD + 1) {
            return false;
        }

        // Detect repetitive tool calls (simplest loop)
        Trace current = history.get(0);
        Span lastToolSpan = getLastToolSpan(current);

        if (lastToolSpan == null) {
            return false;
        }

        int repeatCount = 0;

        // Look back
        for (int i = 1; i < history.size(); i++) {
            Trace previous = history.get(i);
            Span prevToolSpan = getLastToolSpan(previous);

            if (isSameAction(lastToolSpan, prevToolSpan)) {
                repeatCount++;
            } else {
                break;
            }
        }

        boolean loopDetected = repeatCount >= LOOP_THRESHOLD;
        if (loopDetected) {
            log.warn("Agent Loop Detected! Action '{}' repeated {} times.", lastToolSpan.getName(), repeatCount + 1);
        }

        return loopDetected;
    }

    private Span getLastToolSpan(Trace trace) {
        if (trace.getSpans() == null || trace.getSpans().isEmpty())
            return null;

        return trace.getSpans().stream()
                .filter(s -> s.getType() == Span.SpanType.TOOL_CALL)
                .findFirst()
                .orElse(null);
    }

    private boolean isSameAction(Span s1, Span s2) {
        if (s1 == null || s2 == null)
            return false;

        if (!s1.getName().equals(s2.getName()))
            return false;

        // Deep compare arguments
        String input1 = getSpanInput(s1);
        String input2 = getSpanInput(s2);

        if (input1 == null && input2 == null)
            return true;
        if (input1 == null || input2 == null)
            return false;

        return input1.equals(input2);
    }

    private String getSpanInput(Span span) {
        // Try toolInput first
        if (span.getToolInput() != null && span.getToolInput().containsKey("input")) {
            return String.valueOf(span.getToolInput().get("input"));
        }
        // Fallback to metadata
        if (span.getMetadata() != null) {
            return (String) span.getMetadata().get("input");
        }
        return null;
    }
}
