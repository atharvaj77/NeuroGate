package com.neurogate.vault.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Represents a sanitized prompt with PII replaced by tokens
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SanitizedPrompt {
    private String sanitizedText;              // Text with PII replaced by tokens
    private String originalText;               // Original unsanitized text
    private Map<String, String> tokenMap;      // token -> original value mapping
    private List<PiiEntity> detectedEntities;  // List of detected PII entities
    private boolean containsPii;               // Whether PII was detected

    public SanitizedPrompt(String sanitizedText, Map<String, String> tokenMap) {
        this.sanitizedText = sanitizedText;
        this.tokenMap = tokenMap;
        this.containsPii = !tokenMap.isEmpty();
    }
}
