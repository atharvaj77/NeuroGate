package com.neurogate.debugger;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Semantic diff between two LLM responses.
 */
@Data
@Builder
public class SemanticDiff {
    private String sessionId;

    // Original vs comparison
    private String originalResponse;
    private String comparisonResponse;

    // Semantic similarity (0.0 - 1.0)
    private double semanticSimilarity;

    // Text-level differences
    private List<String> textDiffs;

    // Metadata comparison
    private String originalProvider;
    private String comparisonProvider;
    private Double originalCost;
    private Double comparisonCost;
    private long originalLatency;
    private long comparisonLatency;

    // Quality scores (optional)
    private Double originalQualityScore;
    private Double comparisonQualityScore;

    /**
     * Check if responses are semantically similar (>= 90%)
     */
    public boolean areSimilar() {
        return semanticSimilarity >= 0.90;
    }

    /**
     * Get cost savings if switching to comparison provider
     */
    public double getCostSavings() {
        if (originalCost != null && comparisonCost != null) {
            return originalCost - comparisonCost;
        }
        return 0.0;
    }

    /**
     * Get latency improvement
     */
    public long getLatencyImprovement() {
        return originalLatency - comparisonLatency;
    }
}
