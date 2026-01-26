package com.neurogate.router.intelligence.detector;

import com.neurogate.router.intelligence.model.Intent;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects creative writing and content generation intents.
 */
@Component
public class CreativeIntentDetector implements IntentDetector {

    private static final List<WeightedPattern> CREATIVE_PATTERNS = List.of(
            new WeightedPattern("(?i)write\\s+(a\\s+)?(story|poem|essay|article|blog|narrative)", 0.95),
            new WeightedPattern("(?i)(creative|fiction|imaginative)\\s+writing", 0.9),
            new WeightedPattern("(?i)(story|poem|lyrics|screenplay|dialogue)\\s+(about|for)", 0.9),
            new WeightedPattern("(?i)compose\\s+(a\\s+)?", 0.8),
            new WeightedPattern("(?i)(once upon|in a world|imagine)", 0.85)
    );

    private static final List<WeightedPattern> SUMMARIZATION_PATTERNS = List.of(
            new WeightedPattern("(?i)summarize\\s+(the|this)", 0.95),
            new WeightedPattern("(?i)give\\s+(me\\s+)?a\\s+summary", 0.9),
            new WeightedPattern("(?i)tl;?dr", 0.95),
            new WeightedPattern("(?i)condense\\s+(this|the)", 0.85),
            new WeightedPattern("(?i)key\\s+(points|takeaways)", 0.8),
            new WeightedPattern("(?i)main\\s+(ideas|points)", 0.75)
    );

    private static final List<WeightedPattern> TRANSLATION_PATTERNS = List.of(
            new WeightedPattern("(?i)translate\\s+(this|the|to|from|into)", 0.95),
            new WeightedPattern("(?i)in\\s+(spanish|french|german|chinese|japanese|korean|arabic)", 0.8),
            new WeightedPattern("(?i)how\\s+do\\s+you\\s+say", 0.85),
            new WeightedPattern("(?i)convert\\s+to\\s+(spanish|french|german)", 0.8)
    );

    @Override
    public Optional<DetectionResult> detect(String prompt) {
        // Check translation (most specific)
        for (WeightedPattern wp : TRANSLATION_PATTERNS) {
            Matcher matcher = wp.pattern().matcher(prompt);
            if (matcher.find()) {
                return Optional.of(new DetectionResult(Intent.TRANSLATION, wp.weight(), wp.pattern().pattern()));
            }
        }

        // Check summarization
        for (WeightedPattern wp : SUMMARIZATION_PATTERNS) {
            Matcher matcher = wp.pattern().matcher(prompt);
            if (matcher.find()) {
                return Optional.of(new DetectionResult(Intent.SUMMARIZATION, wp.weight(), wp.pattern().pattern()));
            }
        }

        // Check creative writing
        for (WeightedPattern wp : CREATIVE_PATTERNS) {
            Matcher matcher = wp.pattern().matcher(prompt);
            if (matcher.find()) {
                return Optional.of(new DetectionResult(Intent.CREATIVE_WRITING, wp.weight(), wp.pattern().pattern()));
            }
        }

        return Optional.empty();
    }

    @Override
    public int getPriority() {
        return 30;
    }

    @Override
    public String getName() {
        return "creative-intent";
    }

    private record WeightedPattern(Pattern pattern, double weight) {
        WeightedPattern(String regex, double weight) {
            this(Pattern.compile(regex), weight);
        }
    }
}
