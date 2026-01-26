package com.neurogate.vault.streaming;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.regex.Pattern;

/**
 * A pattern for detecting toxic or harmful content in streams.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToxicityPattern {

    /**
     * Compiled regex pattern.
     */
    private Pattern pattern;

    /**
     * Original regex string (for debugging).
     */
    private String regex;

    /**
     * Severity level of this pattern.
     */
    private Severity severity;

    /**
     * Category of toxic content.
     */
    private String category;

    /**
     * Points to add to toxicity score when matched.
     */
    private int toxicityPoints;

    /**
     * Action to take when pattern matches.
     */
    private Action action;

    /**
     * Human-readable description.
     */
    private String description;

    /**
     * Severity levels.
     */
    public enum Severity {
        LOW,      // Minor issue, just log
        MEDIUM,   // Warning, increment toxicity score
        HIGH,     // Critical, may trigger abort
        CRITICAL  // Immediate abort
    }

    /**
     * Actions to take when pattern matches.
     */
    public enum Action {
        LOG,      // Just log the match
        WARN,     // Increment warning counter
        FILTER,   // Remove the content
        ABORT     // Stop the stream immediately
    }

    /**
     * Create a pattern from configuration.
     */
    public static ToxicityPattern fromConfig(
            String regex,
            String severity,
            String category,
            String action,
            int points,
            String description
    ) {
        return ToxicityPattern.builder()
                .regex(regex)
                .pattern(Pattern.compile(regex, Pattern.CASE_INSENSITIVE))
                .severity(Severity.valueOf(severity.toUpperCase()))
                .category(category)
                .action(Action.valueOf(action.toUpperCase()))
                .toxicityPoints(points)
                .description(description)
                .build();
    }
}