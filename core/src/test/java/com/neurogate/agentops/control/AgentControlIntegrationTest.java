package com.neurogate.agentops.control;

import com.neurogate.agentops.model.Span;
import com.neurogate.agentops.model.Trace;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AgentControlIntegrationTest {

    private final LoopDetector loopDetector = new LoopDetector();
    private final ToolHallucinationGuard guard = new ToolHallucinationGuard();

    @Test
    void testLoopDetection() {
        List<Trace> history = new ArrayList<>();

        // Create 4 identical traces
        for (int i = 0; i < 4; i++) {
            Trace t = new Trace();
            Span s = new Span();
            s.setType(Span.SpanType.TOOL_CALL);
            s.setName("web_search");
            // Use metadata for input
            s.setMetadata(Map.of("input", "weather in ny"));
            t.setSpans(List.of(s));
            history.add(0, t); // Add to front (newest first)
        }

        assertTrue(loopDetector.isLooping(history), "Should detect loop");
    }

    @Test
    void testNoLoop_DifferentInputs() {
        List<Trace> history = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            Trace t = new Trace();
            Span s = new Span();
            s.setType(Span.SpanType.TOOL_CALL);
            s.setName("web_search");
            // Different inputs
            s.setMetadata(Map.of("input", "weather in ny " + i));
            t.setSpans(List.of(s));
            history.add(0, t);
        }

        assertFalse(loopDetector.isLooping(history), "Should NOT detect loop with different inputs");
    }

    @Test
    void testHallucinationCorrection() {
        // "search_web" vs "web_search" (distance > threshold maybe? let's check)
        // distance("search_web", "web_search") = 6. Threshold is 3.

        // Try typo: "web_serch" -> "web_search" (distance 1)
        Optional<String> fix = guard.suggestCorrection("web_serch");
        assertTrue(fix.isPresent());
        assertEquals("web_search", fix.get());

        // Try exact
        Optional<String> valid = guard.suggestCorrection("web_search"); // NOTE: REGISTERED_TOOLS has "web_search"
        assertTrue(valid.isEmpty());

        // Try massive hallucination
        Optional<String> none = guard.suggestCorrection("launch_missiles");
        assertTrue(none.isEmpty());
    }
}
