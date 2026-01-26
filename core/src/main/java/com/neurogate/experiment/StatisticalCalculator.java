package com.neurogate.experiment;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.inference.TTest;
import org.springframework.stereotype.Component;

/**
 * Statistical calculations for A/B test analysis.
 * Uses Welch's t-test for comparing means with unequal variances.
 */
@Slf4j
@Component
public class StatisticalCalculator {

    private final TTest tTest = new TTest();

    /**
     * Perform Welch's t-test to compare two samples.
     *
     * @param control   Control group samples
     * @param treatment Treatment group samples
     * @return p-value (probability that difference is due to chance)
     */
    public double welchTTest(double[] control, double[] treatment) {
        if (control.length < 2 || treatment.length < 2) {
            log.warn("Insufficient samples for t-test: control={}, treatment={}",
                    control.length, treatment.length);
            return 1.0; // No significance
        }

        try {
            return tTest.tTest(control, treatment);
        } catch (Exception e) {
            log.error("Error performing t-test: {}", e.getMessage());
            return 1.0;
        }
    }

    /**
     * Check if the difference is statistically significant.
     *
     * @param pValue          The p-value from t-test
     * @param significanceLevel Alpha level (default 0.05)
     * @return true if statistically significant
     */
    public boolean isSignificant(double pValue, double significanceLevel) {
        return pValue < significanceLevel;
    }

    /**
     * Calculate descriptive statistics for a sample.
     */
    public SampleStats calculateStats(double[] samples) {
        if (samples == null || samples.length == 0) {
            return SampleStats.empty();
        }

        DescriptiveStatistics stats = new DescriptiveStatistics(samples);

        return SampleStats.builder()
                .count(samples.length)
                .mean(stats.getMean())
                .stdDev(stats.getStandardDeviation())
                .min(stats.getMin())
                .max(stats.getMax())
                .p50(stats.getPercentile(50))
                .p75(stats.getPercentile(75))
                .p90(stats.getPercentile(90))
                .p95(stats.getPercentile(95))
                .p99(stats.getPercentile(99))
                .build();
    }

    /**
     * Calculate percentage improvement (negative = treatment worse).
     */
    public double calculateImprovementPercent(double controlMean, double treatmentMean) {
        if (controlMean == 0) return 0;
        // For latency/cost, lower is better, so invert
        return ((controlMean - treatmentMean) / controlMean) * 100;
    }

    /**
     * Calculate required sample size for desired statistical power.
     *
     * @param effectSize Expected effect size (Cohen's d)
     * @param power      Desired power (0.8 = 80%)
     * @param alpha      Significance level (0.05 = 5%)
     * @return Required sample size per group
     */
    public int requiredSampleSize(double effectSize, double power, double alpha) {
        // Using approximation formula for two-sample t-test
        // n = 2 * ((z_alpha/2 + z_beta) / d)^2
        // where d is Cohen's d effect size

        double zAlpha = getZScore(1 - alpha / 2);
        double zBeta = getZScore(power);

        if (effectSize == 0) return Integer.MAX_VALUE;

        double n = 2 * Math.pow((zAlpha + zBeta) / effectSize, 2);
        return (int) Math.ceil(n);
    }

    /**
     * Get z-score for a given cumulative probability.
     */
    private double getZScore(double probability) {
        // Approximation using inverse error function
        // For common values:
        if (probability >= 0.975) return 1.96;  // 95% CI
        if (probability >= 0.95) return 1.645;  // 90% CI
        if (probability >= 0.90) return 1.28;
        if (probability >= 0.80) return 0.84;

        // General approximation
        return Math.sqrt(2) * inverseErf(2 * probability - 1);
    }

    private double inverseErf(double x) {
        // Approximation of inverse error function
        double a = 0.147;
        double ln1MinusX2 = Math.log(1 - x * x);
        double term1 = (2 / (Math.PI * a)) + (ln1MinusX2 / 2);
        double term2 = ln1MinusX2 / a;

        return Math.signum(x) * Math.sqrt(Math.sqrt(term1 * term1 - term2) - term1);
    }

    /**
     * Generate recommendation based on statistical analysis.
     */
    public String generateRecommendation(
            double controlMean,
            double treatmentMean,
            double pValue,
            double significanceLevel,
            int controlSamples,
            int treatmentSamples,
            boolean lowerIsBetter
    ) {
        int minSamples = 30; // Minimum for CLT

        if (controlSamples < minSamples || treatmentSamples < minSamples) {
            return "INSUFFICIENT_DATA";
        }

        if (!isSignificant(pValue, significanceLevel)) {
            return "NO_SIGNIFICANT_DIFFERENCE";
        }

        if (lowerIsBetter) {
            return treatmentMean < controlMean ? "TREATMENT_BETTER" : "CONTROL_BETTER";
        } else {
            return treatmentMean > controlMean ? "TREATMENT_BETTER" : "CONTROL_BETTER";
        }
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SampleStats {
        private int count;
        private double mean;
        private double stdDev;
        private double min;
        private double max;
        private double p50;
        private double p75;
        private double p90;
        private double p95;
        private double p99;

        public static SampleStats empty() {
            return SampleStats.builder().count(0).build();
        }
    }
}