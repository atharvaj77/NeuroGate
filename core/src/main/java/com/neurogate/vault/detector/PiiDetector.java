package com.neurogate.vault.detector;

import com.neurogate.vault.model.PiiEntity;

import java.util.List;

/**
 * Interface for detecting PII in text
 */
public interface PiiDetector {

    /**
     * Detect PII entities in the given text
     *
     * @param text Text to scan for PII
     * @return List of detected PII entities
     */
    List<PiiEntity> detect(String text);

    /**
     * Get the name of this detector
     */
    String getName();
}
