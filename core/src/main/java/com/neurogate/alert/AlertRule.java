package com.neurogate.alert;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * AlertRule — user-defined rule that fires when a gateway metric crosses a
 * threshold for a sustained duration.
 *
 * <p>
 * Rules are evaluated every 15 seconds by {@link AlertService}. The state
 * machine is: {@code OK → FIRING → RESOLVED}. A {@code cooldownMinutes}
 * prevents repeated firings within a window.
 */
@Entity
@Table(name = "alert_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRule {

    public enum MetricType {
        LATENCY, // avgLatencyMs
        P95_LATENCY, // p95LatencyMs
        ERROR_RATE, // fraction [0,1]
        COST, // costUsdTotal (cumulative)
        PROVIDER_HEALTH, // error_rate per provider proxy
        CACHE_HIT_RATE, // fraction [0,1]
        RPS // requests per second
    }

    public enum Operator {
        GT, // metric > threshold
        LT // metric < threshold
    }

    public enum AlertChannel {
        LOG, // server-side log only
        SLACK, // Slack incoming webhook
        WEBHOOK // Generic HTTP POST webhook
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MetricType metricType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Operator operator;

    @Column(nullable = false)
    private double threshold;

    /** How long the condition must hold before firing (seconds). */
    @Builder.Default
    @Column(nullable = false)
    private int durationSeconds = 60;

    /** Silence window after a FIRING event (minutes). */
    @Builder.Default
    @Column(nullable = false)
    private int cooldownMinutes = 15;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private AlertChannel channel = AlertChannel.LOG;

    /**
     * JSON string containing channel-specific config.
     * Slack: {@code {"webhookUrl":"https://hooks.slack.com/…"}}
     * Webhook: {@code {"url":"https://…","method":"POST","headers":{}}}
     */
    @Column(columnDefinition = "TEXT")
    private String channelConfig;

    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant lastFiredAt;

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    @PrePersist
    private void prePersist() {
        if (createdAt == null)
            createdAt = Instant.now();
    }

    // -----------------------------------------------------------------------
    // Business logic
    // -----------------------------------------------------------------------

    /**
     * Returns {@code true} if the rule is within its cooldown window and should
     * not fire again yet.
     */
    public boolean isInCooldown() {
        if (lastFiredAt == null)
            return false;
        return Instant.now().isBefore(lastFiredAt.plusSeconds((long) cooldownMinutes * 60));
    }

    /**
     * Returns {@code true} if the given {@code metricValue} violates this rule.
     */
    public boolean isViolated(double metricValue) {
        return switch (operator) {
            case GT -> metricValue > threshold;
            case LT -> metricValue < threshold;
        };
    }
}
