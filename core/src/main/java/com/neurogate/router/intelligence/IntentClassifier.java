package com.neurogate.router.intelligence;

import com.neurogate.router.intelligence.model.Intent;
import com.neurogate.router.intelligence.model.IntentClassification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Classifies prompt intent for intelligent model routing.
 * Uses pattern matching and complexity analysis.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntentClassifier {

    private final ComplexityAnalyzer complexityAnalyzer;

    // Intent detection patterns with weights
    private static final Map<Intent, List<WeightedPattern>> INTENT_PATTERNS = new EnumMap<>(Intent.class);

    static {
        // Code Generation patterns
        INTENT_PATTERNS.put(Intent.CODE_GENERATION, List.of(
                new WeightedPattern("(?i)write\\s+(a\\s+)?(function|class|method|code|script|program)", 0.9),
                new WeightedPattern("(?i)create\\s+(a\\s+)?(function|class|api|endpoint|service)", 0.9),
                new WeightedPattern("(?i)implement\\s+(a\\s+)?", 0.8),
                new WeightedPattern("(?i)generate\\s+(a\\s+)?(code|function|class)", 0.9),
                new WeightedPattern("(?i)build\\s+(a\\s+)?(function|class|component|module)", 0.8),
                new WeightedPattern("(?i)code\\s+(for|to|that)", 0.7)
        ));

        // Code Review patterns
        INTENT_PATTERNS.put(Intent.CODE_REVIEW, List.of(
                new WeightedPattern("(?i)(fix|debug|review|refactor)\\s+(this|the|my|our)", 0.9),
                new WeightedPattern("(?i)what'?s\\s+wrong\\s+with", 0.9),
                new WeightedPattern("(?i)why\\s+(is|does|doesn't)\\s+(this|it)\\s+(not\\s+work|fail|error)", 0.9),
                new WeightedPattern("(?i)find\\s+(the\\s+)?(bug|error|issue|problem)", 0.85),
                new WeightedPattern("(?i)improve\\s+(this|the|my)\\s+code", 0.8),
                new WeightedPattern("(?i)optimize\\s+(this|the|my)", 0.75)
        ));

        // Code Explanation patterns
        INTENT_PATTERNS.put(Intent.CODE_EXPLANATION, List.of(
                new WeightedPattern("(?i)explain\\s+(this|the|how|what)", 0.85),
                new WeightedPattern("(?i)what\\s+does\\s+(this|the)\\s+(code|function|class)", 0.9),
                new WeightedPattern("(?i)how\\s+does\\s+(this|the)\\s+(code|function|work)", 0.9),
                new WeightedPattern("(?i)walk\\s+(me\\s+)?through", 0.8),
                new WeightedPattern("(?i)break\\s+down\\s+(this|the)", 0.75)
        ));

        // Reasoning patterns
        INTENT_PATTERNS.put(Intent.REASONING, List.of(
                new WeightedPattern("(?i)analyze\\s+(the|this|why|how)", 0.8),
                new WeightedPattern("(?i)step[- ]by[- ]step", 0.85),
                new WeightedPattern("(?i)think\\s+(through|about)", 0.8),
                new WeightedPattern("(?i)reason\\s+(about|through|why)", 0.9),
                new WeightedPattern("(?i)derive\\s+(the|a)", 0.85),
                new WeightedPattern("(?i)prove\\s+(that|the)", 0.9),
                new WeightedPattern("(?i)compare\\s+and\\s+contrast", 0.8),
                new WeightedPattern("(?i)evaluate\\s+(the|this|whether)", 0.75)
        ));

        // Math/Science patterns
        INTENT_PATTERNS.put(Intent.MATH_SCIENCE, List.of(
                new WeightedPattern("(?i)solve\\s+(the|this|for)", 0.9),
                new WeightedPattern("(?i)calculate\\s+(the|this)", 0.9),
                new WeightedPattern("(?i)\\d+\\s*[+\\-*/^]\\s*\\d+", 0.7),
                new WeightedPattern("(?i)(equation|formula|integral|derivative)", 0.85),
                new WeightedPattern("(?i)(physics|chemistry|biology|math|calculus|algebra)", 0.8),
                new WeightedPattern("(?i)what\\s+is\\s+\\d+", 0.6)
        ));

        // Creative Writing patterns
        INTENT_PATTERNS.put(Intent.CREATIVE_WRITING, List.of(
                new WeightedPattern("(?i)write\\s+(a\\s+)?(story|poem|essay|article|blog|narrative)", 0.95),
                new WeightedPattern("(?i)(creative|fiction|imaginative)\\s+writing", 0.9),
                new WeightedPattern("(?i)(story|poem|lyrics|screenplay|dialogue)\\s+(about|for)", 0.9),
                new WeightedPattern("(?i)compose\\s+(a\\s+)?", 0.8),
                new WeightedPattern("(?i)(once upon|in a world|imagine)", 0.85)
        ));

        // Summarization patterns
        INTENT_PATTERNS.put(Intent.SUMMARIZATION, List.of(
                new WeightedPattern("(?i)summarize\\s+(the|this)", 0.95),
                new WeightedPattern("(?i)give\\s+(me\\s+)?a\\s+summary", 0.9),
                new WeightedPattern("(?i)tl;?dr", 0.95),
                new WeightedPattern("(?i)condense\\s+(this|the)", 0.85),
                new WeightedPattern("(?i)key\\s+(points|takeaways)", 0.8),
                new WeightedPattern("(?i)main\\s+(ideas|points)", 0.75)
        ));

        // Translation patterns
        INTENT_PATTERNS.put(Intent.TRANSLATION, List.of(
                new WeightedPattern("(?i)translate\\s+(this|the|to|from|into)", 0.95),
                new WeightedPattern("(?i)in\\s+(spanish|french|german|chinese|japanese|korean|arabic)", 0.8),
                new WeightedPattern("(?i)how\\s+do\\s+you\\s+say", 0.85),
                new WeightedPattern("(?i)convert\\s+to\\s+(spanish|french|german)", 0.8)
        ));

        // Q&A patterns
        INTENT_PATTERNS.put(Intent.QUESTION_ANSWERING, List.of(
                new WeightedPattern("(?i)^(what|who|where|when|which)\\s+(is|are|was|were)", 0.8),
                new WeightedPattern("(?i)^(do|does|did|can|could|will|would)\\s+", 0.7),
                new WeightedPattern("(?i)^(is|are)\\s+(it|this|that|there)", 0.7),
                new WeightedPattern("(?i)tell\\s+me\\s+(about|what)", 0.75)
        ));

        // Data Analysis patterns
        INTENT_PATTERNS.put(Intent.DATA_ANALYSIS, List.of(
                new WeightedPattern("(?i)analyze\\s+(this\\s+)?(data|dataset|csv|json|table)", 0.9),
                new WeightedPattern("(?i)(statistics|statistical|metrics|trends)", 0.8),
                new WeightedPattern("(?i)(chart|graph|plot|visualize)", 0.75),
                new WeightedPattern("(?i)find\\s+(patterns|correlations|insights)", 0.85)
        ));

        // Instruction Following patterns
        INTENT_PATTERNS.put(Intent.INSTRUCTION_FOLLOWING, List.of(
                new WeightedPattern("(?i)^(please\\s+)?(do|make|perform|execute|run)", 0.7),
                new WeightedPattern("(?i)^(follow|complete|finish)\\s+(these|the|this)", 0.8),
                new WeightedPattern("(?i)step\\s+\\d+:", 0.75)
        ));

        // Conversation (fallback, low weights)
        INTENT_PATTERNS.put(Intent.CONVERSATION, List.of(
                new WeightedPattern("(?i)^(hi|hello|hey|howdy|greetings)", 0.9),
                new WeightedPattern("(?i)^(how\\s+are\\s+you|what's\\s+up)", 0.85),
                new WeightedPattern("(?i)^(thanks|thank\\s+you)", 0.7),
                new WeightedPattern("(?i)chat\\s+(with|about)", 0.75)
        ));
    }

    /**
     * Classify the intent of a prompt.
     */
    public IntentClassification classify(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return IntentClassification.builder()
                    .intent(Intent.CONVERSATION)
                    .confidence(0.0)
                    .allScores(Map.of())
                    .build();
        }

        // Get complexity analysis
        ComplexityScore complexity = complexityAnalyzer.analyze(prompt);

        // Calculate pattern-based scores
        Map<Intent, Double> scores = new EnumMap<>(Intent.class);
        List<String> matchedFeatures = new ArrayList<>();

        for (Intent intent : Intent.values()) {
            double score = calculateIntentScore(prompt, intent, matchedFeatures);
            scores.put(intent, score);
        }

        // Adjust scores based on complexity analysis
        adjustScoresForComplexity(scores, complexity);

        // Find the highest scoring intent
        Intent topIntent = scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(Intent.CONVERSATION);

        double topScore = scores.get(topIntent);

        // Normalize confidence to 0-1 range
        double confidence = Math.min(1.0, topScore);

        log.debug("Intent classification: {} (confidence: {:.2f}), complexity: {}",
                topIntent, confidence, complexity);

        return IntentClassification.builder()
                .intent(topIntent)
                .confidence(confidence)
                .allScores(scores)
                .complexityScore(complexity)
                .matchedFeatures(matchedFeatures)
                .build();
    }

    private double calculateIntentScore(String prompt, Intent intent, List<String> matchedFeatures) {
        List<WeightedPattern> patterns = INTENT_PATTERNS.get(intent);
        if (patterns == null) return 0.0;

        double maxScore = 0.0;

        for (WeightedPattern wp : patterns) {
            Matcher matcher = wp.pattern.matcher(prompt);
            if (matcher.find()) {
                if (wp.weight > maxScore) {
                    maxScore = wp.weight;
                    matchedFeatures.add(intent.name() + ":" + wp.pattern.pattern());
                }
            }
        }

        return maxScore;
    }

    private void adjustScoresForComplexity(Map<Intent, Double> scores, ComplexityScore complexity) {
        // Boost reasoning for high reasoning complexity
        if (complexity.getReasoning() >= 7) {
            scores.merge(Intent.REASONING, 0.2, Double::sum);
        }

        // Boost code intents for high domain complexity (coding)
        if (complexity.getDomain() >= 7) {
            scores.merge(Intent.CODE_GENERATION, 0.15, Double::sum);
            scores.merge(Intent.CODE_REVIEW, 0.15, Double::sum);
            scores.merge(Intent.CODE_EXPLANATION, 0.1, Double::sum);
        }

        // Boost creative for high creativity scores
        if (complexity.getCreativity() >= 7) {
            scores.merge(Intent.CREATIVE_WRITING, 0.2, Double::sum);
        }

        // Boost summarization for longer expected outputs
        if (complexity.getOutputLength() >= 6) {
            scores.merge(Intent.SUMMARIZATION, 0.1, Double::sum);
        }
    }

    /**
     * Pattern with associated weight.
     */
    private static class WeightedPattern {
        final Pattern pattern;
        final double weight;

        WeightedPattern(String regex, double weight) {
            this.pattern = Pattern.compile(regex);
            this.weight = weight;
        }
    }
}