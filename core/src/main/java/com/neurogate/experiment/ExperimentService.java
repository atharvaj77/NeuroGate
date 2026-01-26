package com.neurogate.experiment;

import com.neurogate.experiment.model.*;
import com.neurogate.sentinel.model.ChatRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Service for managing A/B test experiments.
 * Handles experiment lifecycle, variant assignment, and result collection.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExperimentService {

    private final StatisticalCalculator statisticalCalculator;

    // In-memory storage (use database for production)
    private final Map<String, Experiment> experiments = new ConcurrentHashMap<>();
    private final Map<String, List<ExperimentResult>> results = new ConcurrentHashMap<>();

    // Significance level for statistical tests
    private static final double SIGNIFICANCE_LEVEL = 0.05;

    /**
     * Create a new experiment.
     */
    public Experiment createExperiment(CreateExperimentRequest request) {
        String experimentId = UUID.randomUUID().toString().substring(0, 8);

        Experiment experiment = Experiment.builder()
                .experimentId(experimentId)
                .name(request.getName())
                .description(request.getDescription())
                .controlModel(request.getControlModel())
                .treatmentModel(request.getTreatmentModel())
                .trafficSplitPercent(request.getTrafficSplitPercent())
                .targetSampleSize(request.getTargetSampleSize())
                .primaryMetric(request.getPrimaryMetric())
                .status(ExperimentStatus.DRAFT)
                .enabled(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        experiments.put(experimentId, experiment);
        results.put(experimentId, new CopyOnWriteArrayList<>());

        log.info("Created experiment: {} - {} vs {}", experimentId,
                request.getControlModel(), request.getTreatmentModel());

        if (request.isStartImmediately()) {
            return startExperiment(experimentId);
        }

        return experiment;
    }

    /**
     * Start an experiment.
     */
    public Experiment startExperiment(String experimentId) {
        Experiment experiment = getExperimentOrThrow(experimentId);

        experiment.setStatus(ExperimentStatus.RUNNING);
        experiment.setEnabled(true);
        experiment.setStartTime(Instant.now());
        experiment.setUpdatedAt(Instant.now());

        log.info("Started experiment: {}", experimentId);
        return experiment;
    }

    /**
     * Pause an experiment.
     */
    public Experiment pauseExperiment(String experimentId) {
        Experiment experiment = getExperimentOrThrow(experimentId);

        experiment.setStatus(ExperimentStatus.PAUSED);
        experiment.setEnabled(false);
        experiment.setUpdatedAt(Instant.now());

        log.info("Paused experiment: {}", experimentId);
        return experiment;
    }

    /**
     * Stop an experiment.
     */
    public Experiment stopExperiment(String experimentId) {
        Experiment experiment = getExperimentOrThrow(experimentId);

        experiment.setStatus(ExperimentStatus.STOPPED);
        experiment.setEnabled(false);
        experiment.setEndTime(Instant.now());
        experiment.setUpdatedAt(Instant.now());

        log.info("Stopped experiment: {}", experimentId);
        return experiment;
    }

    /**
     * Get experiment by ID.
     */
    public Optional<Experiment> getExperiment(String experimentId) {
        return Optional.ofNullable(experiments.get(experimentId));
    }

    /**
     * List all experiments.
     */
    public List<Experiment> listExperiments() {
        return new ArrayList<>(experiments.values());
    }

    /**
     * List active (running) experiments.
     */
    public List<Experiment> listActiveExperiments() {
        return experiments.values().stream()
                .filter(Experiment::isEnabled)
                .filter(e -> e.getStatus() == ExperimentStatus.RUNNING)
                .toList();
    }

    /**
     * Find an active experiment that matches the request.
     * For now, returns the first active experiment. Can be extended to support
     * targeting rules (by user, model, etc.).
     */
    public Optional<Experiment> findActiveExperiment(ChatRequest request) {
        // In production, implement targeting logic here
        // e.g., match by requested model, user segment, etc.
        return listActiveExperiments().stream().findFirst();
    }

    /**
     * Assign a variant to a request.
     * Uses deterministic hashing for consistent assignment.
     */
    public Variant assignVariant(String experimentId, ChatRequest request) {
        Experiment experiment = getExperimentOrThrow(experimentId);

        // Create a deterministic hash based on request characteristics
        String hashKey = experimentId + ":" +
                (request.getUser() != null ? request.getUser() : UUID.randomUUID().toString());

        int hash = Math.abs(hashKey.hashCode());
        int bucket = hash % 100;

        // If bucket < trafficSplitPercent, assign to treatment
        Variant variant = bucket < experiment.getTrafficSplitPercent()
                ? Variant.TREATMENT
                : Variant.CONTROL;

        log.debug("Assigned variant {} for experiment {} (bucket: {})",
                variant, experimentId, bucket);

        return variant;
    }

    /**
     * Get the model for a given variant in an experiment.
     */
    public String getModelForVariant(String experimentId, Variant variant) {
        Experiment experiment = getExperimentOrThrow(experimentId);
        return variant == Variant.CONTROL
                ? experiment.getControlModel()
                : experiment.getTreatmentModel();
    }

    /**
     * Record an experiment result.
     */
    public void recordResult(ExperimentResult result) {
        if (result.getExperimentId() == null) {
            log.warn("Cannot record result without experiment ID");
            return;
        }

        result.setResultId(UUID.randomUUID().toString().substring(0, 8));
        result.setTimestamp(Instant.now());

        List<ExperimentResult> experimentResults = results.get(result.getExperimentId());
        if (experimentResults != null) {
            experimentResults.add(result);

            // Check if target sample size reached
            Experiment experiment = experiments.get(result.getExperimentId());
            if (experiment != null && experiment.getTargetSampleSize() != null) {
                if (experimentResults.size() >= experiment.getTargetSampleSize()) {
                    log.info("Experiment {} reached target sample size: {}",
                            result.getExperimentId(), experiment.getTargetSampleSize());
                    experiment.setStatus(ExperimentStatus.COMPLETED);
                    experiment.setEnabled(false);
                    experiment.setEndTime(Instant.now());
                }
            }
        }

        log.debug("Recorded result for experiment {}: variant={}, latency={}ms, cost=${}",
                result.getExperimentId(), result.getVariant(),
                result.getLatencyMs(), result.getCostUsd());
    }

    /**
     * Get raw results for an experiment.
     */
    public List<ExperimentResult> getResults(String experimentId) {
        return results.getOrDefault(experimentId, List.of());
    }

    /**
     * Calculate comprehensive statistics for an experiment.
     */
    public ExperimentStats calculateStats(String experimentId) {
        Experiment experiment = getExperimentOrThrow(experimentId);
        List<ExperimentResult> allResults = getResults(experimentId);

        // Split by variant
        List<ExperimentResult> controlResults = allResults.stream()
                .filter(r -> r.getVariant() == Variant.CONTROL)
                .toList();

        List<ExperimentResult> treatmentResults = allResults.stream()
                .filter(r -> r.getVariant() == Variant.TREATMENT)
                .toList();

        // Extract metrics arrays
        double[] controlLatencies = controlResults.stream()
                .mapToDouble(ExperimentResult::getLatencyMs).toArray();
        double[] treatmentLatencies = treatmentResults.stream()
                .mapToDouble(ExperimentResult::getLatencyMs).toArray();

        double[] controlCosts = controlResults.stream()
                .mapToDouble(ExperimentResult::getCostUsd).toArray();
        double[] treatmentCosts = treatmentResults.stream()
                .mapToDouble(ExperimentResult::getCostUsd).toArray();

        // Calculate stats
        StatisticalCalculator.SampleStats controlLatencyStats =
                statisticalCalculator.calculateStats(controlLatencies);
        StatisticalCalculator.SampleStats treatmentLatencyStats =
                statisticalCalculator.calculateStats(treatmentLatencies);
        StatisticalCalculator.SampleStats controlCostStats =
                statisticalCalculator.calculateStats(controlCosts);
        StatisticalCalculator.SampleStats treatmentCostStats =
                statisticalCalculator.calculateStats(treatmentCosts);

        // Statistical tests
        double latencyPValue = statisticalCalculator.welchTTest(controlLatencies, treatmentLatencies);
        double costPValue = statisticalCalculator.welchTTest(controlCosts, treatmentCosts);

        // Success rates
        double controlSuccessRate = controlResults.isEmpty() ? 0 :
                controlResults.stream().filter(ExperimentResult::isSuccess).count() /
                        (double) controlResults.size();
        double treatmentSuccessRate = treatmentResults.isEmpty() ? 0 :
                treatmentResults.stream().filter(ExperimentResult::isSuccess).count() /
                        (double) treatmentResults.size();

        // Generate recommendation
        String recommendation = statisticalCalculator.generateRecommendation(
                controlLatencyStats.getMean(),
                treatmentLatencyStats.getMean(),
                latencyPValue,
                SIGNIFICANCE_LEVEL,
                controlResults.size(),
                treatmentResults.size(),
                true // Lower latency is better
        );

        // Calculate improvements
        double latencyImprovement = statisticalCalculator.calculateImprovementPercent(
                controlLatencyStats.getMean(), treatmentLatencyStats.getMean());
        double costImprovement = statisticalCalculator.calculateImprovementPercent(
                controlCostStats.getMean(), treatmentCostStats.getMean());

        // Generate summary
        String summary = generateSummary(experiment, controlResults.size(),
                treatmentResults.size(), latencyImprovement, costImprovement,
                latencyPValue, recommendation);

        return ExperimentStats.builder()
                .experimentId(experimentId)
                .experimentName(experiment.getName())
                .controlSamples(controlResults.size())
                .treatmentSamples(treatmentResults.size())
                .totalSamples(allResults.size())
                // Latency stats
                .controlLatencyMean(controlLatencyStats.getMean())
                .controlLatencyStdDev(controlLatencyStats.getStdDev())
                .controlLatencyP50(controlLatencyStats.getP50())
                .controlLatencyP95(controlLatencyStats.getP95())
                .controlLatencyP99(controlLatencyStats.getP99())
                .treatmentLatencyMean(treatmentLatencyStats.getMean())
                .treatmentLatencyStdDev(treatmentLatencyStats.getStdDev())
                .treatmentLatencyP50(treatmentLatencyStats.getP50())
                .treatmentLatencyP95(treatmentLatencyStats.getP95())
                .treatmentLatencyP99(treatmentLatencyStats.getP99())
                // Cost stats
                .controlCostMean(controlCostStats.getMean())
                .controlCostTotal(Arrays.stream(controlCosts).sum())
                .treatmentCostMean(treatmentCostStats.getMean())
                .treatmentCostTotal(Arrays.stream(treatmentCosts).sum())
                // Success rates
                .controlSuccessRate(controlSuccessRate)
                .treatmentSuccessRate(treatmentSuccessRate)
                // Statistical significance
                .latencyPValue(latencyPValue)
                .costPValue(costPValue)
                .confidenceLevel(1 - SIGNIFICANCE_LEVEL)
                .statisticallySignificant(latencyPValue < SIGNIFICANCE_LEVEL)
                // Recommendations
                .recommendation(recommendation)
                .summary(summary)
                .latencyImprovementPercent(latencyImprovement)
                .costImprovementPercent(costImprovement)
                .build();
    }

    /**
     * Delete an experiment and its results.
     */
    public void deleteExperiment(String experimentId) {
        experiments.remove(experimentId);
        results.remove(experimentId);
        log.info("Deleted experiment: {}", experimentId);
    }

    private Experiment getExperimentOrThrow(String experimentId) {
        return getExperiment(experimentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Experiment not found: " + experimentId));
    }

    private String generateSummary(
            Experiment experiment,
            int controlSamples,
            int treatmentSamples,
            double latencyImprovement,
            double costImprovement,
            double pValue,
            String recommendation
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Experiment '%s': %s vs %s. ",
                experiment.getName(),
                experiment.getControlModel(),
                experiment.getTreatmentModel()));

        sb.append(String.format("Samples: %d control, %d treatment. ",
                controlSamples, treatmentSamples));

        if ("INSUFFICIENT_DATA".equals(recommendation)) {
            sb.append("More data needed for statistical significance (min 30 per group).");
        } else if ("NO_SIGNIFICANT_DIFFERENCE".equals(recommendation)) {
            sb.append(String.format("No significant difference detected (p=%.3f). ", pValue));
        } else {
            String winner = "TREATMENT_BETTER".equals(recommendation)
                    ? experiment.getTreatmentModel()
                    : experiment.getControlModel();
            sb.append(String.format("%s performs better. ", winner));
            sb.append(String.format("Latency: %.1f%% improvement. ", Math.abs(latencyImprovement)));
            sb.append(String.format("Cost: %.1f%% improvement. ", Math.abs(costImprovement)));
            sb.append(String.format("(p=%.4f, statistically significant)", pValue));
        }

        return sb.toString();
    }
}