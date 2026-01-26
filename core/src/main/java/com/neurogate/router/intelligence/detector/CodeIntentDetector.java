package com.neurogate.router.intelligence.detector;

import com.neurogate.router.intelligence.model.Intent;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects code-related intents: generation, review, and explanation.
 */
@Component
public class CodeIntentDetector implements IntentDetector {

    private static final List<WeightedPattern> CODE_GENERATION_PATTERNS = List.of(
            new WeightedPattern("(?i)write\\s+(a\\s+)?(function|class|method|code|script|program)", 0.9),
            new WeightedPattern("(?i)create\\s+(a\\s+)?(function|class|api|endpoint|service)", 0.9),
            new WeightedPattern("(?i)implement\\s+(a\\s+)?", 0.8),
            new WeightedPattern("(?i)generate\\s+(a\\s+)?(code|function|class)", 0.9),
            new WeightedPattern("(?i)build\\s+(a\\s+)?(function|class|component|module)", 0.8),
            new WeightedPattern("(?i)code\\s+(for|to|that)", 0.7)
    );

    private static final List<WeightedPattern> CODE_REVIEW_PATTERNS = List.of(
            new WeightedPattern("(?i)(fix|debug|review|refactor)\\s+(this|the|my|our)", 0.9),
            new WeightedPattern("(?i)what'?s\\s+wrong\\s+with", 0.9),
            new WeightedPattern("(?i)why\\s+(is|does|doesn't)\\s+(this|it)\\s+(not\\s+work|fail|error)", 0.9),
            new WeightedPattern("(?i)find\\s+(the\\s+)?(bug|error|issue|problem)", 0.85),
            new WeightedPattern("(?i)improve\\s+(this|the|my)\\s+code", 0.8),
            new WeightedPattern("(?i)optimize\\s+(this|the|my)", 0.75)
    );

    private static final List<WeightedPattern> CODE_EXPLANATION_PATTERNS = List.of(
            new WeightedPattern("(?i)explain\\s+(this|the|how|what)", 0.85),
            new WeightedPattern("(?i)what\\s+does\\s+(this|the)\\s+(code|function|class)", 0.9),
            new WeightedPattern("(?i)how\\s+does\\s+(this|the)\\s+(code|function|work)", 0.9),
            new WeightedPattern("(?i)walk\\s+(me\\s+)?through", 0.8),
            new WeightedPattern("(?i)break\\s+down\\s+(this|the)", 0.75)
    );

    @Override
    public Optional<DetectionResult> detect(String prompt) {
        // Check code generation first (highest priority for code tasks)
        Optional<DetectionResult> generation = matchPatterns(prompt, CODE_GENERATION_PATTERNS, Intent.CODE_GENERATION);
        if (generation.isPresent()) return generation;

        // Check code review
        Optional<DetectionResult> review = matchPatterns(prompt, CODE_REVIEW_PATTERNS, Intent.CODE_REVIEW);
        if (review.isPresent()) return review;

        // Check code explanation
        return matchPatterns(prompt, CODE_EXPLANATION_PATTERNS, Intent.CODE_EXPLANATION);
    }

    private Optional<DetectionResult> matchPatterns(String prompt, List<WeightedPattern> patterns, Intent intent) {
        for (WeightedPattern wp : patterns) {
            Matcher matcher = wp.pattern().matcher(prompt);
            if (matcher.find()) {
                return Optional.of(new DetectionResult(intent, wp.weight(), wp.pattern().pattern()));
            }
        }
        return Optional.empty();
    }

    @Override
    public int getPriority() {
        return 10; // High priority - code detection is common
    }

    @Override
    public String getName() {
        return "code-intent";
    }

    private record WeightedPattern(Pattern pattern, double weight) {
        WeightedPattern(String regex, double weight) {
            this(Pattern.compile(regex), weight);
        }
    }
}
