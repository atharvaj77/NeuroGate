package com.neurogate.prompts;

import lombok.Builder;
import lombok.Data;

/**
 * A/B test results comparing two prompt versions.
 */
@Data
@Builder
public class ABTestResult {
    private String versionIdA;
    private String versionIdB;

    private ABTestMetrics metricsA;
    private ABTestMetrics metricsB;

    private String winner; // "A" or "B"
    private String recommendation; // Human-readable recommendation

    /**
     * Get cost savings if switching to winner
     */
    public double getCostSavings() {
        if ("A".equals(winner)) {
            return metricsB.getAverageCost() - metricsA.getAverageCost();
        } else {
            return metricsA.getAverageCost() - metricsB.getAverageCost();
        }
    }

    /**
     * Get latency improvement if switching to winner
     */
    public double getLatencyImprovement() {
        if ("A".equals(winner)) {
            return metricsB.getAverageLatency() - metricsA.getAverageLatency();
        } else {
            return metricsA.getAverageLatency() - metricsB.getAverageLatency();
        }
    }
}
