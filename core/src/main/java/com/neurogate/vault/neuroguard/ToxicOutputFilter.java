package com.neurogate.vault.neuroguard;

import com.neurogate.vault.neuroguard.model.ThreatDetectionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Filters toxic or harmful content from LLM responses.
 */
@Slf4j
@Component
public class ToxicOutputFilter {

    private static final List<Pattern> HARMFUL_PATTERNS = List.of(
            // Weapons/Violence
            Pattern.compile("(?i)how to (make|build|create) (a )?(bomb|explosive|weapon)"),
            Pattern.compile("(?i)instructions (for|to) (harm|hurt|kill)"),
            Pattern.compile("(?i)step[- ]?by[- ]?step (guide|instructions) (to|for) (attack|hack)"),

            // Illegal activities
            Pattern.compile("(?i)how to (hack|break into|steal)"),
            Pattern.compile("(?i)(synthesize|manufacture|produce) (drugs|meth|cocaine)"),
            Pattern.compile("(?i)bypass (security|authentication|password)"),

            // Self-harm
            Pattern.compile("(?i)methods? (of|for|to) (suicide|self[- ]?harm)"),
            Pattern.compile("(?i)how to (hurt|harm|end) yourself"),

            // Hate speech indicators
            Pattern.compile("(?i)(all|every) \\b(jews|muslims|blacks|whites)\\b.*(should|must|need to).*"),
            Pattern.compile("(?i)\\b(inferior|subhuman)\\b.*(race|people|group)"));

    // Content to redact (replaced with placeholders)
    private static final List<Pattern> REDACT_PATTERNS = List.of(
            Pattern.compile("(?i)(social security|ssn)[:\\s]*\\d{3}[- ]?\\d{2}[- ]?\\d{4}"),
            Pattern.compile("(?i)(credit card|cc)[:\\s]*\\d{4}[- ]?\\d{4}[- ]?\\d{4}[- ]?\\d{4}"),
            Pattern.compile("(?i)(password|passwd|pwd)[:\\s]*[^\\s]+"));

    /**
     * Analyze output for toxic content
     */
    public ThreatDetectionResult analyze(String output) {
        if (output == null || output.isBlank()) {
            return ThreatDetectionResult.safe();
        }

        List<String> matchedPatterns = new ArrayList<>();
        double confidenceScore = 0.0;

        // Check harmful patterns
        for (Pattern pattern : HARMFUL_PATTERNS) {
            if (pattern.matcher(output).find()) {
                matchedPatterns.add(pattern.pattern());
                confidenceScore += 0.4;
            }
        }

        confidenceScore = Math.min(confidenceScore, 1.0);

        if (matchedPatterns.isEmpty()) {
            return ThreatDetectionResult.safe();
        }

        log.warn("Toxic output detected: {} patterns matched, confidence: {}",
                matchedPatterns.size(), confidenceScore);

        return ThreatDetectionResult.builder()
                .threatDetected(true)
                .threatType(ThreatDetectionResult.ThreatType.TOXIC_CONTENT)
                .confidenceScore(confidenceScore)
                .matchedPatterns(matchedPatterns)
                .blocked(confidenceScore >= 0.5)
                .message("Potentially harmful content detected in response")
                .build();
    }

    /**
     * Sanitize output by redacting sensitive information
     */
    public String sanitize(String output) {
        if (output == null) {
            return null;
        }

        String sanitized = output;
        for (Pattern pattern : REDACT_PATTERNS) {
            sanitized = pattern.matcher(sanitized).replaceAll("[REDACTED]");
        }

        return sanitized;
    }
}
