package com.neurogate.analytics;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Tracks per-user/team/project LLM usage for cost allocation.
 */
@Entity
@Table(name = "usage_records", indexes = {
        @Index(name = "idx_user_id", columnList = "userId"),
        @Index(name = "idx_team_id", columnList = "teamId"),
        @Index(name = "idx_timestamp", columnList = "timestamp")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * User who made the request
     */
    @Column(nullable = false)
    private String userId;

    /**
     * Team/department (optional)
     */
    private String teamId;

    /**
     * Project identifier (optional)
     */
    private String projectId;

    /**
     * Provider used (openai, anthropic, gemini)
     */
    @Column(nullable = false)
    private String provider;

    /**
     * Model used (gpt-4, claude-3-opus, gemini-pro, etc.)
     */
    @Column(nullable = false)
    private String model;

    /**
     * Prompt tokens consumed
     */
    @Column(nullable = false)
    private Integer promptTokens;

    /**
     * Completion tokens generated
     */
    @Column(nullable = false)
    private Integer completionTokens;

    /**
     * Total tokens (prompt + completion)
     */
    @Column(nullable = false)
    private Integer totalTokens;

    /**
     * Cost in USD
     */
    @Column(nullable = false, precision = 10, scale = 6)
    private BigDecimal costUsd;

    /**
     * Was this a cache hit?
     */
    @Column(nullable = false)
    private Boolean cacheHit;

    /**
     * Request timestamp
     */
    @Column(nullable = false)
    private Instant timestamp;

    /**
     * Request ID for tracing
     */
    @Column(nullable = false)
    private String requestId;

    /**
     * Request latency in milliseconds
     */
    private Long latencyMs;

    /**
     * Complexity score (if available)
     */
    private Integer complexityScore;
}
