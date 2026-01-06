package com.neurogate.prompts;

import lombok.Builder;
import lombok.Data;

/**
 * Metrics for A/B test variant.
 */
@Data
@Builder
public class ABTestMetrics {
    private int requestCount;
    private double averageLatency; // milliseconds
    private double averageCost; // USD
    private double successRate; // 0.0 - 1.0
    private double p95Latency;
    private double p99Latency;
}
