package com.neurogate.debugger;

import lombok.Builder;
import lombok.Data;

/**
 * Options for replaying a request with modifications.
 */
@Data
@Builder
public class ReplayOptions {
    // Override model
    private String model;

    // Override temperature
    private Double temperature;

    // Override max tokens
    private Integer maxTokens;

    // Bypass cache (force fresh call)
    @Builder.Default
    private boolean bypassCache = false;

    // Override provider
    private String provider;

    // Additional metadata
    private String replayReason;
}
