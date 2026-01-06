package com.neurogate.prompts;

import lombok.Builder;
import lombok.Data;

/**
 * Result of a branch merge operation.
 */
@Data
@Builder
public class MergeResult {
    private boolean success;
    private boolean conflictDetected;
    private double similarity;
    private String mergedVersionId;
    private String message;
    private String conflictResolution; // How to resolve if conflict
}
