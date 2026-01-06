package com.neurogate.vault.neuroguard.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Result of security analysis.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThreatDetectionResult {

    private boolean threatDetected;

    @JsonProperty("threat_type")
    private ThreatType threatType;

    @JsonProperty("confidence_score")
    private double confidenceScore;

    private String message;

    @JsonProperty("matched_patterns")
    private List<String> matchedPatterns;

    @JsonProperty("sanitized_content")
    private String sanitizedContent;

    private boolean blocked;

    public enum ThreatType {
        NONE,
        PROMPT_INJECTION,
        JAILBREAK,
        TOXIC_CONTENT,
        PII_LEAK,
        DATA_EXFILTRATION
    }

    public static ThreatDetectionResult safe() {
        return ThreatDetectionResult.builder()
                .threatDetected(false)
                .threatType(ThreatType.NONE)
                .confidenceScore(0.0)
                .blocked(false)
                .build();
    }
}
