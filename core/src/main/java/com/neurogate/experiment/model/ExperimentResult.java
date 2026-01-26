package com.neurogate.experiment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * A single observation from an A/B test experiment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentResult {

    private String resultId;
    private String experimentId;
    private String requestId;

    /**
     * Which variant this request was assigned to.
     */
    private Variant variant;

    /**
     * The model that was actually used.
     */
    private String modelUsed;

    /**
     * End-to-end latency in milliseconds.
     */
    private long latencyMs;

    /**
     * Input tokens used.
     */
    private int inputTokens;

    /**
     * Output tokens generated.
     */
    private int outputTokens;

    /**
     * Total cost in USD.
     */
    private double costUsd;

    /**
     * Optional quality score from Cortex evaluation (0-100).
     */
    private Double qualityScore;

    /**
     * Whether the request succeeded.
     */
    @Builder.Default
    private boolean success = true;

    /**
     * Error message if request failed.
     */
    private String errorMessage;

    private Instant timestamp;
}