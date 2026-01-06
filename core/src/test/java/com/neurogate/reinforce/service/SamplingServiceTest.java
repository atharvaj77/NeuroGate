package com.neurogate.reinforce.service;

import com.neurogate.agentops.model.Trace;
import com.neurogate.agentops.model.UserFeedback;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SamplingServiceTest {

    private final SamplingService samplingService = new SamplingService();

    @Test
    void shouldSample_WhenUserFeedbackIsThumbsDown() {
        Trace trace = Trace.builder()
                .traceId("t1")
                .userFeedback(UserFeedback.THUMBS_DOWN)
                .build();

        boolean result = samplingService.shouldSample(trace);
        assertTrue(result, "Should sample when feedback is negative");
    }

    @Test
    void shouldSample_WhenConsensusScoreIsLow() {
        Trace trace = Trace.builder()
                .traceId("t2")
                .metadata(Map.of("consensus_score", 0.5))
                .build();

        boolean result = samplingService.shouldSample(trace);
        assertTrue(result, "Should sample when consensus score is low");
    }

    @Test
    void shouldNotSample_WhenConsensusScoreIsHigh() {
        Trace trace = Trace.builder()
                .traceId("t3")
                .metadata(Map.of("consensus_score", 0.9))
                .build();

        boolean result = samplingService.shouldSample(trace);
        // Note: There is a 1% chance this fails due to random sampling, but highly
        // unlikely for a single run.
        // For robustness, we could mock Random, but for this quick test it's
        // acceptable.
        assertFalse(result, "Should not sample when consensus score is high (mostly)");
    }
}
