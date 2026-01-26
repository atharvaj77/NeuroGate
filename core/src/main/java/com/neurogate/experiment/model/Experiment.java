package com.neurogate.experiment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * A/B test experiment configuration.
 * Compares performance between control and treatment models.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Experiment {

    private String experimentId;
    private String name;
    private String description;

    /**
     * The control model (baseline).
     */
    private String controlModel;

    /**
     * The treatment model (challenger).
     */
    private String treatmentModel;

    /**
     * Percentage of traffic routed to treatment (0-100).
     * E.g., 50 means 50% control, 50% treatment.
     */
    @Builder.Default
    private int trafficSplitPercent = 50;

    /**
     * Whether the experiment is actively routing traffic.
     */
    @Builder.Default
    private boolean enabled = false;

    @Builder.Default
    private ExperimentStatus status = ExperimentStatus.DRAFT;

    private Instant startTime;
    private Instant endTime;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Optional: Target number of samples before auto-completion.
     */
    private Integer targetSampleSize;

    /**
     * Optional: Metric to optimize (latency, cost, quality).
     */
    @Builder.Default
    private String primaryMetric = "latency";
}