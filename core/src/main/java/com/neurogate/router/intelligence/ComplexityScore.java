package com.neurogate.router.intelligence;

import lombok.Builder;
import lombok.Data;

/**
 * Multi-dimensional complexity analysis for intelligent model routing.
 * Scores range from 1-10 for each dimension.
 */
@Data
@Builder
public class ComplexityScore {

    /**
     * Reasoning complexity (1-10)
     * Low: Simple calculations, lookups
     * High: Multi-step reasoning, analysis
     */
    private int reasoning;

    /**
     * Domain knowledge required (1-10)
     * Low: General knowledge
     * High: Specialized expertise
     */
    private int domain;

    /**
     * Expected output length (1-10)
     * Low: Single word/sentence
     * High: Essay/long-form content
     */
    private int outputLength;

    /**
     * Creativity required (1-10)
     * Low: Factual responses
     * High: Creative/imaginative content
     */
    private int creativity;

    /**
     * Overall calculated score based on weighted ensemble
     */
    private int overallScore;

    /**
     * Get overall complexity score
     */
    public int getOverallScore() {
        return overallScore;
    }

    /**
     * Recommend model based on overall complexity score
     *
     * Score ranges:
     * - 8-10: GPT-4 / Claude Opus (complex reasoning)
     * - 4-7: GPT-3.5 / Claude Sonnet (balanced)
     * - 1-3: Local SLM / Claude Haiku (simple tasks)
     */
    public String getRecommendedModel() {
        int score = getOverallScore();

        if (score >= 8) {
            return "gpt-4"; // Most expensive, best quality
        } else if (score >= 4) {
            return "gpt-3.5-turbo"; // Balanced cost/quality
        } else {
            return "local-llm"; // Cheapest (local)
        }
    }

    /**
     * Get recommended provider based on score
     */
    public String getRecommendedProvider() {
        int score = getOverallScore();

        if (score >= 8) {
            return "openai"; // GPT-4
        } else if (score >= 6) {
            return "anthropic"; // Claude Sonnet
        } else if (score >= 4) {
            return "gemini"; // Gemini Pro (cheapest cloud option)
        } else {
            return "local"; // Local SLM
        }
    }

    /**
     * Get estimated cost (in USD) for this request
     * Based on average 500 tokens per request
     */
    public double getEstimatedCost() {
        String model = getRecommendedModel();

        switch (model) {
            case "gpt-4":
                return 0.03; // $0.03 per request (500 tokens)
            case "gpt-3.5-turbo":
                return 0.001; // $0.001 per request
            case "local-llm":
                return 0.0; // Free
            default:
                return 0.01;
        }
    }
}
