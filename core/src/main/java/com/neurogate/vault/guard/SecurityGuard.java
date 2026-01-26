package com.neurogate.vault.guard;

import com.neurogate.vault.neuroguard.model.ThreatDetectionResult;

/**
 * Chain of Responsibility interface for security guards.
 * Each guard checks for specific security threats.
 */
public interface SecurityGuard {

    /**
     * Check content for security threats.
     *
     * @param content The content to analyze
     * @return Detection result
     */
    ThreatDetectionResult check(String content);

    /**
     * Get the guard type for identification.
     */
    GuardType getType();

    /**
     * Get the priority (lower = runs first).
     */
    default int getPriority() {
        return 100;
    }

    /**
     * Types of security guards.
     */
    enum GuardType {
        PII_DETECTION,
        PROMPT_INJECTION,
        JAILBREAK,
        TOXIC_CONTENT,
        CUSTOM
    }
}
