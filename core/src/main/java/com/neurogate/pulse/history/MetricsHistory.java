package com.neurogate.pulse.history;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * MetricsHistory — one-minute aggregated snapshot of Pulse metrics stored in
 * the {@code metrics_history} table.
 */
@Entity
@Table(name = "metrics_history", indexes = {
        @Index(name = "idx_metrics_history_bucket", columnList = "bucketTime DESC")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricsHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Truncated to the nearest minute (the start of the bucket). */
    @Column(nullable = false)
    private Instant bucketTime;

    @Column(nullable = false)
    private double rps;

    @Column(nullable = false)
    private double avgLatencyMs;

    @Column(nullable = false)
    private double p95LatencyMs;

    /** Fraction [0, 1]. */
    @Column(nullable = false)
    private double errorRate;

    @Column(nullable = false)
    private long tokenCount;

    /** Fraction [0, 1]. */
    @Column(nullable = false)
    private double cacheHitRate;

    @Column(nullable = false)
    private double costUsd;

    @Column(nullable = false)
    private long piiBlocked;

    private String provider;
}
