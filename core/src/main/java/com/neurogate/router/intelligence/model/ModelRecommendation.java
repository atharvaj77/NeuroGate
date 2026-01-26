package com.neurogate.router.intelligence.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A model recommendation for a specific intent.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelRecommendation {

    /**
     * The model identifier (e.g., "gpt-4o", "claude-3-5-sonnet-20241022").
     */
    private String model;

    /**
     * Priority order (1 = highest priority).
     */
    private int priority;

    /**
     * Human-readable reason for this recommendation.
     */
    private String reason;

    /**
     * Whether this model is currently available.
     */
    @Builder.Default
    private boolean available = true;
}