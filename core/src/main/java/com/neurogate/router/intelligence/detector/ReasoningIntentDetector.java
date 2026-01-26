package com.neurogate.router.intelligence.detector;

import com.neurogate.router.intelligence.model.Intent;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects reasoning and analytical intents.
 */
@Component
public class ReasoningIntentDetector implements IntentDetector {

    private static final List<WeightedPattern> REASONING_PATTERNS = List.of(
            new WeightedPattern("(?i)analyze\\s+(the|this|why|how)", 0.8),
            new WeightedPattern("(?i)step[- ]by[- ]step", 0.85),
            new WeightedPattern("(?i)think\\s+(through|about)", 0.8),
            new WeightedPattern("(?i)reason\\s+(about|through|why)", 0.9),
            new WeightedPattern("(?i)derive\\s+(the|a)", 0.85),
            new WeightedPattern("(?i)prove\\s+(that|the)", 0.9),
            new WeightedPattern("(?i)compare\\s+and\\s+contrast", 0.8),
            new WeightedPattern("(?i)evaluate\\s+(the|this|whether)", 0.75)
    );

    private static final List<WeightedPattern> MATH_SCIENCE_PATTERNS = List.of(
            new WeightedPattern("(?i)solve\\s+(the|this|for)", 0.9),
            new WeightedPattern("(?i)calculate\\s+(the|this)", 0.9),
            new WeightedPattern("(?i)\\d+\\s*[+\\-*/^]\\s*\\d+", 0.7),
            new WeightedPattern("(?i)(equation|formula|integral|derivative)", 0.85),
            new WeightedPattern("(?i)(physics|chemistry|biology|math|calculus|algebra)", 0.8),
            new WeightedPattern("(?i)what\\s+is\\s+\\d+", 0.6)
    );

    @Override
    public Optional<DetectionResult> detect(String prompt) {
        // Check math/science first
        for (WeightedPattern wp : MATH_SCIENCE_PATTERNS) {
            Matcher matcher = wp.pattern().matcher(prompt);
            if (matcher.find()) {
                return Optional.of(new DetectionResult(Intent.MATH_SCIENCE, wp.weight(), wp.pattern().pattern()));
            }
        }

        // Check general reasoning
        for (WeightedPattern wp : REASONING_PATTERNS) {
            Matcher matcher = wp.pattern().matcher(prompt);
            if (matcher.find()) {
                return Optional.of(new DetectionResult(Intent.REASONING, wp.weight(), wp.pattern().pattern()));
            }
        }

        return Optional.empty();
    }

    @Override
    public int getPriority() {
        return 20;
    }

    @Override
    public String getName() {
        return "reasoning-intent";
    }

    private record WeightedPattern(Pattern pattern, double weight) {
        WeightedPattern(String regex, double weight) {
            this(Pattern.compile(regex), weight);
        }
    }
}
