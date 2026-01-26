package com.neurogate.vault.streaming;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for streaming content guardrails.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "neurogate.streaming-guardrails")
public class StreamingGuardrailConfig {

    /**
     * Whether streaming guardrails are enabled.
     */
    private boolean enabled = true;

    /**
     * Toxicity threshold (0-100) that triggers abort.
     */
    private int toxicityThreshold = 70;

    /**
     * Buffer size for pattern matching context.
     */
    private int bufferSize = 500;

    /**
     * Maximum warnings before abort.
     */
    private int maxWarnings = 5;

    /**
     * Custom pattern configurations.
     */
    private List<PatternConfig> patterns;

    /**
     * Built patterns (compiled after initialization).
     */
    private List<ToxicityPattern> compiledPatterns;

    @Data
    public static class PatternConfig {
        private String pattern;
        private String severity;
        private String category;
        private String action;
        private int points;
        private String description;
    }

    @PostConstruct
    public void init() {
        compiledPatterns = new ArrayList<>();

        // Add custom patterns from config
        if (patterns != null) {
            for (PatternConfig pc : patterns) {
                try {
                    compiledPatterns.add(ToxicityPattern.fromConfig(
                            pc.getPattern(),
                            pc.getSeverity(),
                            pc.getCategory(),
                            pc.getAction(),
                            pc.getPoints(),
                            pc.getDescription()
                    ));
                } catch (Exception e) {
                    // Invalid pattern, skip
                }
            }
        }

        // Add default patterns
        addDefaultPatterns();
    }

    private void addDefaultPatterns() {
        // Prompt injection attempts
        compiledPatterns.add(ToxicityPattern.fromConfig(
                "(?i)(ignore|disregard|forget)\\s+(all\\s+)?(previous|prior|above)\\s+(instructions?|rules?|prompts?)",
                "CRITICAL", "PROMPT_INJECTION", "ABORT", 100,
                "Attempt to override system instructions"
        ));

        compiledPatterns.add(ToxicityPattern.fromConfig(
                "(?i)you\\s+are\\s+now\\s+(in|a)\\s+(developer|admin|unrestricted|jailbreak)",
                "CRITICAL", "JAILBREAK", "ABORT", 100,
                "Jailbreak attempt"
        ));

        compiledPatterns.add(ToxicityPattern.fromConfig(
                "(?i)(DAN|do\\s+anything\\s+now)\\s+mode",
                "CRITICAL", "JAILBREAK", "ABORT", 100,
                "DAN jailbreak pattern"
        ));

        // Harmful content patterns
        compiledPatterns.add(ToxicityPattern.fromConfig(
                "(?i)how\\s+to\\s+(make|build|create)\\s+(a\\s+)?(bomb|explosive|weapon)",
                "CRITICAL", "HARMFUL_INSTRUCTIONS", "ABORT", 100,
                "Request for dangerous instructions"
        ));

        compiledPatterns.add(ToxicityPattern.fromConfig(
                "(?i)(step[- ]by[- ]step|instructions?\\s+for)\\s+(harm|hurt|kill|attack)",
                "CRITICAL", "HARMFUL_INSTRUCTIONS", "ABORT", 100,
                "Step-by-step harmful instructions"
        ));

        // Medium severity patterns
        compiledPatterns.add(ToxicityPattern.fromConfig(
                "(?i)(bypass|circumvent|avoid)\\s+(security|safety|content)\\s+(filter|policy|restriction)",
                "HIGH", "SECURITY_BYPASS", "WARN", 40,
                "Attempt to bypass security measures"
        ));

        compiledPatterns.add(ToxicityPattern.fromConfig(
                "(?i)(pretend|act\\s+like|roleplay\\s+as)\\s+.{0,20}(no\\s+rules|unrestricted|evil)",
                "HIGH", "ROLEPLAY_BYPASS", "WARN", 35,
                "Roleplay to bypass restrictions"
        ));

        // Low severity patterns (monitoring)
        compiledPatterns.add(ToxicityPattern.fromConfig(
                "(?i)(hypothetically|theoretically|in\\s+fiction)\\s+.{0,30}(illegal|harmful|dangerous)",
                "MEDIUM", "HYPOTHETICAL_HARMFUL", "WARN", 20,
                "Hypothetical harmful scenario"
        ));

        compiledPatterns.add(ToxicityPattern.fromConfig(
                "(?i)(educational|research)\\s+purposes?\\s+only",
                "LOW", "DISCLAIMER_PATTERN", "LOG", 5,
                "Common disclaimer pattern"
        ));
    }
}