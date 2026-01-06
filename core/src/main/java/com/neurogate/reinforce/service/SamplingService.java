package com.neurogate.reinforce.service;

import com.neurogate.agentops.model.Trace;
import com.neurogate.agentops.model.UserFeedback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class SamplingService {

    private final Random random = new Random();

    /**
     * Decides whether a trace should be sampled for human review.
     */
    public boolean shouldSample(Trace trace) {
        // 1. Explicit User Feedback (High Priority)
        // If user gave thumbs down, we MUST review it.
        if (trace.getUserFeedback() == UserFeedback.THUMBS_DOWN) {
            log.info("Sampling trace {} due to negative user feedback", trace.getTraceId());
            return true;
        }

        // 2. Low Consensus Score (if available in metadata)
        if (trace.getMetadata() != null && trace.getMetadata().containsKey("consensus_score")) {
            Object scoreObj = trace.getMetadata().get("consensus_score");
            try {
                double score = Double.parseDouble(scoreObj.toString());
                if (score < 0.7) {
                    log.info("Sampling trace {} due to low consensus score: {}", trace.getTraceId(), score);
                    return true;
                }
            } catch (NumberFormatException e) {
                // Ignore parsing errors
            }
        }

        // 3. Random Sample (1%)
        // Random audit to catch unknown unknowns.
        if (random.nextDouble() < 0.01) {
            log.info("Sampling trace {} for random audit", trace.getTraceId());
            return true;
        }

        return false;
    }

    public String determineSource(Trace trace) {
        if (trace.getUserFeedback() == UserFeedback.THUMBS_DOWN) {
            return "USER_FLAG";
        }
        if (trace.getMetadata() != null && trace.getMetadata().containsKey("consensus_score")) {
            try {
                double score = Double.parseDouble(trace.getMetadata().get("consensus_score").toString());
                if (score < 0.7)
                    return "LOW_CONFIDENCE";
            } catch (Exception ignored) {
            }
        }
        return "RANDOM";
    }
}
