package com.neurogate.experiment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Aggregated statistics for an A/B test experiment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentStats {

    private String experimentId;
    private String experimentName;

    // Sample counts
    private int controlSamples;
    private int treatmentSamples;
    private int totalSamples;

    // Latency statistics (milliseconds)
    private double controlLatencyMean;
    private double controlLatencyStdDev;
    private double controlLatencyP50;
    private double controlLatencyP95;
    private double controlLatencyP99;

    private double treatmentLatencyMean;
    private double treatmentLatencyStdDev;
    private double treatmentLatencyP50;
    private double treatmentLatencyP95;
    private double treatmentLatencyP99;

    // Cost statistics (USD)
    private double controlCostMean;
    private double controlCostTotal;
    private double treatmentCostMean;
    private double treatmentCostTotal;

    // Quality statistics (if available)
    private Double controlQualityMean;
    private Double treatmentQualityMean;

    // Success rates
    private double controlSuccessRate;
    private double treatmentSuccessRate;

    // Statistical significance
    private double latencyPValue;
    private double costPValue;
    private Double qualityPValue;

    private double confidenceLevel;
    private boolean statisticallySignificant;

    /**
     * Recommendation based on analysis.
     * CONTROL_BETTER, TREATMENT_BETTER, NO_SIGNIFICANT_DIFFERENCE, INSUFFICIENT_DATA
     */
    private String recommendation;

    /**
     * Human-readable summary of the results.
     */
    private String summary;

    /**
     * Estimated improvement (positive = treatment better).
     */
    private double latencyImprovementPercent;
    private double costImprovementPercent;
    private Double qualityImprovementPercent;
}