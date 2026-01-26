package com.neurogate.router.intelligence.model;

import com.neurogate.router.intelligence.ComplexityScore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Result of intent classification for a prompt.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntentClassification {

    /**
     * The primary detected intent.
     */
    private Intent intent;

    /**
     * Confidence score (0.0 - 1.0).
     */
    private double confidence;

    /**
     * Scores for all intents (for debugging/analysis).
     */
    private Map<Intent, Double> allScores;

    /**
     * The underlying complexity analysis.
     */
    private ComplexityScore complexityScore;

    /**
     * Features that contributed to the classification.
     */
    private java.util.List<String> matchedFeatures;
}