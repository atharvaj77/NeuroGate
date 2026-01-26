package com.neurogate.router.intelligence.detector;

import com.neurogate.router.intelligence.model.Intent;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects general intents: Q&A, data analysis, conversation, instructions.
 * This is the fallback detector with lowest priority.
 */
@Component
public class GeneralIntentDetector implements IntentDetector {

    private static final List<WeightedPattern> QA_PATTERNS = List.of(
            new WeightedPattern("(?i)^(what|who|where|when|which)\\s+(is|are|was|were)", 0.8),
            new WeightedPattern("(?i)^(do|does|did|can|could|will|would)\\s+", 0.7),
            new WeightedPattern("(?i)^(is|are)\\s+(it|this|that|there)", 0.7),
            new WeightedPattern("(?i)tell\\s+me\\s+(about|what)", 0.75)
    );

    private static final List<WeightedPattern> DATA_ANALYSIS_PATTERNS = List.of(
            new WeightedPattern("(?i)analyze\\s+(this\\s+)?(data|dataset|csv|json|table)", 0.9),
            new WeightedPattern("(?i)(statistics|statistical|metrics|trends)", 0.8),
            new WeightedPattern("(?i)(chart|graph|plot|visualize)", 0.75),
            new WeightedPattern("(?i)find\\s+(patterns|correlations|insights)", 0.85)
    );

    private static final List<WeightedPattern> INSTRUCTION_PATTERNS = List.of(
            new WeightedPattern("(?i)^(please\\s+)?(do|make|perform|execute|run)", 0.7),
            new WeightedPattern("(?i)^(follow|complete|finish)\\s+(these|the|this)", 0.8),
            new WeightedPattern("(?i)step\\s+\\d+:", 0.75)
    );

    private static final List<WeightedPattern> CONVERSATION_PATTERNS = List.of(
            new WeightedPattern("(?i)^(hi|hello|hey|howdy|greetings)", 0.9),
            new WeightedPattern("(?i)^(how\\s+are\\s+you|what's\\s+up)", 0.85),
            new WeightedPattern("(?i)^(thanks|thank\\s+you)", 0.7),
            new WeightedPattern("(?i)chat\\s+(with|about)", 0.75)
    );

    @Override
    public Optional<DetectionResult> detect(String prompt) {
        // Check data analysis
        for (WeightedPattern wp : DATA_ANALYSIS_PATTERNS) {
            Matcher matcher = wp.pattern().matcher(prompt);
            if (matcher.find()) {
                return Optional.of(new DetectionResult(Intent.DATA_ANALYSIS, wp.weight(), wp.pattern().pattern()));
            }
        }

        // Check Q&A
        for (WeightedPattern wp : QA_PATTERNS) {
            Matcher matcher = wp.pattern().matcher(prompt);
            if (matcher.find()) {
                return Optional.of(new DetectionResult(Intent.QUESTION_ANSWERING, wp.weight(), wp.pattern().pattern()));
            }
        }

        // Check instructions
        for (WeightedPattern wp : INSTRUCTION_PATTERNS) {
            Matcher matcher = wp.pattern().matcher(prompt);
            if (matcher.find()) {
                return Optional.of(new DetectionResult(Intent.INSTRUCTION_FOLLOWING, wp.weight(), wp.pattern().pattern()));
            }
        }

        // Check conversation (lowest priority fallback)
        for (WeightedPattern wp : CONVERSATION_PATTERNS) {
            Matcher matcher = wp.pattern().matcher(prompt);
            if (matcher.find()) {
                return Optional.of(new DetectionResult(Intent.CONVERSATION, wp.weight(), wp.pattern().pattern()));
            }
        }

        return Optional.empty();
    }

    @Override
    public int getPriority() {
        return 100; // Lowest priority - fallback detector
    }

    @Override
    public String getName() {
        return "general-intent";
    }

    private record WeightedPattern(Pattern pattern, double weight) {
        WeightedPattern(String regex, double weight) {
            this(Pattern.compile(regex), weight);
        }
    }
}
