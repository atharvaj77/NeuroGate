package com.neurogate.debugger;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Filter for searching debug records.
 */
@Data
@Builder
public class DebugSearchFilter {
    // User/application filters
    private String userId;
    private String applicationId;

    // Provider filter
    private String provider;

    // Time range
    private Instant startTime;
    private Instant endTime;

    // Cost/latency filters
    private Double minCost;
    private Double maxCost;
    private Long minLatency;
    private Long maxLatency;

    // Cache hit filter
    private Boolean cacheHit;

    // PII detected filter
    private Boolean containsPii;

    // Limit results
    @Builder.Default
    private Integer limit = 100;

    // Sort order
    @Builder.Default
    private String sortBy = "timestamp";

    @Builder.Default
    private String sortOrder = "desc";
}
