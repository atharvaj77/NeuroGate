package com.neurogate.router.intelligence;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Analyzes prompts recommendation optimal model routing.
 * Uses heuristic statistical analysis.
 */
@Slf4j
@Service
public class ComplexityAnalyzer {

    // Expanded Feature Sets with Weights
    private static final Map<String, Integer> REASONING_FEATURES = Map.ofEntries(
            Map.entry("analyze", 3), Map.entry("explain", 2), Map.entry("evaluate", 3),
            Map.entry("step-by-step", 4), Map.entry("compare", 2), Map.entry("contrast", 2),
            Map.entry("derive", 4), Map.entry("prove", 5), Map.entry("solve", 3),
            Map.entry("optimize", 4), Map.entry("architecture", 3), Map.entry("implications", 3),
            Map.entry("strategy", 3), Map.entry("critical", 2), Map.entry("logic", 3));

    private static final Map<String, Integer> CODING_FEATURES = Map.ofEntries(
            Map.entry("function", 3), Map.entry("class", 3), Map.entry("method", 2),
            Map.entry("api", 3), Map.entry("bug", 4), Map.entry("fix", 3),
            Map.entry("error", 3), Map.entry("compile", 4), Map.entry("runtime", 4),
            Map.entry("exception", 3), Map.entry("java", 2), Map.entry("python", 2),
            Map.entry("typescript", 2), Map.entry("sql", 3), Map.entry("database", 2),
            Map.entry("docker", 3), Map.entry("kubernetes", 4), Map.entry("aws", 3));

    private static final Map<String, Integer> CREATIVE_FEATURES = Map.ofEntries(
            Map.entry("story", 4), Map.entry("poem", 4), Map.entry("narrative", 3),
            Map.entry("character", 3), Map.entry("plot", 3), Map.entry("imagine", 4),
            Map.entry("screenplay", 5), Map.entry("dialogue", 3), Map.entry("lyrics", 4),
            Map.entry("style", 2), Map.entry("tone", 2), Map.entry("metaphor", 3));

    // Regex Patterns for Pattern Matching Features
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```|`[^`]+`");
    private static final Pattern MATH_PATTERN = Pattern.compile("[0-9+\\-*/()=<>]{5,}");
    private static final Pattern COMPLEX_SENTENCE_PATTERN = Pattern
            .compile("(?i)(however|moreover|consequently|furthermore|although|despite)");

    /**
     * Analyze prompt complexity using statistical feature weighting
     */
    public ComplexityScore analyze(String prompt) {
        String lowerPrompt = prompt.toLowerCase();
        int wordCount = prompt.split("\\s+").length;

        // Calculate Feature Vectors
        double reasoningScore = calculateFeatureScore(lowerPrompt, REASONING_FEATURES);
        double codingScore = calculateFeatureScore(lowerPrompt, CODING_FEATURES);
        double creativeScore = calculateFeatureScore(lowerPrompt, CREATIVE_FEATURES);

        // Pattern Adjustments
        if (CODE_BLOCK_PATTERN.matcher(prompt).find())
            codingScore += 15.0;
        if (MATH_PATTERN.matcher(prompt).find())
            reasoningScore += 10.0;
        long complexConnectives = COMPLEX_SENTENCE_PATTERN.matcher(prompt).results().count();
        reasoningScore += (complexConnectives * 2.0);

        // Normalize Scores to 1-10 Scale
        int normReasoning = normalize(reasoningScore + (codingScore * 0.5)); // Coding implies reasoning
        int normDomain = normalize(codingScore + (wordCount * 0.05)); // Coding & length imply domain depth
        int normOutput = normalize(wordCount * 0.15 + (prompt.contains("comprehensive") ? 5 : 0));
        int normCreativity = normalize(creativeScore);

        // Ensemble Weighting
        int overallScore = (int) Math.round(
                (normReasoning * 0.45) +
                        (normDomain * 0.25) +
                        (normCreativity * 0.15) +
                        (normOutput * 0.15));

        return ComplexityScore.builder()
                .reasoning(normReasoning)
                .domain(normDomain)
                .outputLength(normOutput)
                .creativity(normCreativity)
                .overallScore(overallScore) // Ensure this field exists in ComplexityScore model or remove if transient
                .build();
    }

    private double calculateFeatureScore(String text, Map<String, Integer> featureWeights) {
        double score = 0;
        for (Map.Entry<String, Integer> entry : featureWeights.entrySet()) {
            if (text.contains(entry.getKey())) {
                score += entry.getValue();
            }
        }
        return score;
    }

    private int normalize(double rawScore) {

        int scaled = (int) (1 + Math.log1p(rawScore) * 2.5);
        return Math.max(1, Math.min(10, scaled));
    }
}
