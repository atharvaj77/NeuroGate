package com.neurogate.vault.neuroguard;

import com.neurogate.vault.neuroguard.model.ThreatDetectionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Detects prompt injection attacks using pattern matching.
 */
@Slf4j
@Component
public class PromptInjectionDetector {

    // Patterns for prompt injection detection
    private static final List<Pattern> INJECTION_PATTERNS = List.of(
            // Instruction override
            Pattern.compile("(?i)ignore (all |previous |above |prior )?instructions?"),
            Pattern.compile("(?i)disregard (all |previous |above )?instructions?"),
            Pattern.compile("(?i)forget (everything|all|previous|your instructions)"),

            // Role manipulation
            Pattern.compile("(?i)you are (now|actually) (a |an )?"),
            Pattern.compile("(?i)pretend (you are|to be)"),
            Pattern.compile("(?i)act as (if you were|a|an)"),
            Pattern.compile("(?i)roleplay as"),

            // System prompt extraction
            Pattern.compile("(?i)what (is|are) your (system|initial) (prompt|instructions)"),
            Pattern.compile("(?i)reveal your (system |hidden )?prompt"),
            Pattern.compile("(?i)show me your (original |system )?instructions"),

            // Delimiter exploitation
            Pattern.compile("(?i)```system"),
            Pattern.compile("(?i)\\[INST\\]"),
            Pattern.compile("(?i)<\\|system\\|>"),
            Pattern.compile("(?i)### (instruction|system)"),

            // Developer mode / DAN
            Pattern.compile("(?i)developer mode"),
            Pattern.compile("(?i)jailbreak(ed)? mode"),
            Pattern.compile("(?i)\\bDAN\\b"),
            Pattern.compile("(?i)do anything now"));

    // Higher-risk patterns (block immediately)
    private static final List<Pattern> HIGH_RISK_PATTERNS = List.of(
            Pattern.compile("(?i)ignore all safety"),
            Pattern.compile("(?i)bypass (all |your )?filters?"),
            Pattern.compile("(?i)disable (all |your )?content filter"),
            Pattern.compile("(?i)remove (all )?restrictions"));

    /**
     * Analyze prompt for injection attempts
     */
    public ThreatDetectionResult analyze(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return ThreatDetectionResult.safe();
        }

        List<String> matchedPatterns = new ArrayList<>();
        double confidenceScore = 0.0;
        boolean shouldBlock = false;

        // Check high-risk patterns first
        for (Pattern pattern : HIGH_RISK_PATTERNS) {
            if (pattern.matcher(prompt).find()) {
                matchedPatterns.add(pattern.pattern());
                confidenceScore += 0.4;
                shouldBlock = true;
            }
        }

        // Check standard injection patterns
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(prompt).find()) {
                matchedPatterns.add(pattern.pattern());
                confidenceScore += 0.2;
            }
        }

        // Cap confidence at 1.0
        confidenceScore = Math.min(confidenceScore, 1.0);

        if (matchedPatterns.isEmpty()) {
            return ThreatDetectionResult.safe();
        }

        // Lower threshold to 0.2 to catch single-pattern matches (like "ignore
        // instructions")
        boolean threatDetected = confidenceScore >= 0.2;

        log.warn("Prompt injection detected: {} patterns matched, confidence: {}",
                matchedPatterns.size(), confidenceScore);

        return ThreatDetectionResult.builder()
                .threatDetected(threatDetected)
                .threatType(ThreatDetectionResult.ThreatType.PROMPT_INJECTION)
                .confidenceScore(confidenceScore)
                .matchedPatterns(matchedPatterns)
                .blocked(shouldBlock || confidenceScore >= 0.7)
                .message("Potential prompt injection detected")
                .build();
    }
}
