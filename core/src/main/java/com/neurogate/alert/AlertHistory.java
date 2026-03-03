package com.neurogate.alert;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * AlertHistory — records every state transition (FIRING / RESOLVED / TEST) for
 * an {@link AlertRule}.
 */
@Entity
@Table(name = "alert_history", indexes = {
        @Index(name = "idx_alert_history_rule", columnList = "ruleId, firedAt DESC"),
        @Index(name = "idx_alert_history_status", columnList = "status, firedAt DESC")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertHistory {

    public enum AlertStatus {
        FIRING,
        RESOLVED,
        TEST
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Foreign key to the rule that fired. */
    @Column(nullable = false)
    private UUID ruleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertStatus status;

    /** The actual metric value that triggered the alert. */
    private Double metricValue;

    /** Human-readable message included in notifications. */
    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    @Builder.Default
    private Instant firedAt = Instant.now();

    private Instant resolvedAt;
}
