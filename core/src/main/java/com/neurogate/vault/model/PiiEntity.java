package com.neurogate.vault.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a detected PII entity in text
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PiiEntity {
    private PiiType type;
    private String value;        // The actual PII value (e.g., "john@example.com")
    private int start;           // Start position in text
    private int end;             // End position in text
    private double confidence;   // Confidence score (0.0 to 1.0)

    public PiiEntity(PiiType type, String value, int start, int end) {
        this(type, value, start, end, 1.0);
    }
}
