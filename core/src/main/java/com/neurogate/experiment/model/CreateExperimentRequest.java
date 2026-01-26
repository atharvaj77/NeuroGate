package com.neurogate.experiment.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to create a new A/B test experiment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateExperimentRequest {

    @NotBlank(message = "Experiment name is required")
    private String name;

    private String description;

    @NotBlank(message = "Control model is required")
    private String controlModel;

    @NotBlank(message = "Treatment model is required")
    private String treatmentModel;

    @Min(value = 1, message = "Traffic split must be at least 1%")
    @Max(value = 99, message = "Traffic split must be at most 99%")
    @Builder.Default
    private int trafficSplitPercent = 50;

    /**
     * Optional: Target sample size for auto-completion.
     */
    private Integer targetSampleSize;

    /**
     * Primary metric to optimize: latency, cost, quality.
     */
    @Builder.Default
    private String primaryMetric = "latency";

    /**
     * Start immediately after creation.
     */
    @Builder.Default
    private boolean startImmediately = false;
}