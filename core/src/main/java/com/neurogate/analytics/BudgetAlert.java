package com.neurogate.analytics;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Tracks budget alerts for users/teams.
 */
@Entity
@Table(name = "budget_alerts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * User or team identifier
     */
    @Column(nullable = false)
    private String entityId;

    /**
     * Entity type: USER or TEAM
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EntityType entityType;

    /**
     * Budget period: DAILY, WEEKLY, MONTHLY
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private BudgetPeriod period;

    /**
     * Budget limit in USD
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal budgetLimit;

    /**
     * Current spending in this period
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal currentSpending;

    /**
     * Alert threshold percentage (e.g., 80 = alert at 80% of budget)
     */
    @Column(nullable = false)
    private Integer alertThreshold;

    /**
     * Has alert been triggered?
     */
    @Column(nullable = false)
    private Boolean alertTriggered;

    /**
     * Should requests be throttled when budget exceeded?
     */
    @Column(nullable = false)
    private Boolean enableThrottling;

    /**
     * Period start time
     */
    @Column(nullable = false)
    private Instant periodStart;

    /**
     * Period end time
     */
    @Column(nullable = false)
    private Instant periodEnd;

    /**
     * Last updated timestamp
     */
    @Column(nullable = false)
    private Instant lastUpdated;

    public enum EntityType {
        USER, TEAM
    }

    public enum BudgetPeriod {
        DAILY, WEEKLY, MONTHLY
    }

    // --- Domain Logic ---

    public static BudgetAlert createForUser(String userId, BigDecimal limit, BudgetPeriod period, int threshold,
            boolean throttle) {
        Instant now = Instant.now();
        return BudgetAlert.builder()
                .entityId(userId)
                .entityType(EntityType.USER)
                .period(period)
                .budgetLimit(limit)
                .currentSpending(BigDecimal.ZERO)
                .alertThreshold(threshold)
                .alertTriggered(false)
                .enableThrottling(throttle)
                .periodStart(now)
                .periodEnd(calculatePeriodEnd(now, period))
                .lastUpdated(now)
                .build();
    }

    public static BudgetAlert createForTeam(String teamId, BigDecimal limit, BudgetPeriod period, int threshold,
            boolean throttle) {
        Instant now = Instant.now();
        return BudgetAlert.builder()
                .entityId(teamId)
                .entityType(EntityType.TEAM)
                .period(period)
                .budgetLimit(limit)
                .currentSpending(BigDecimal.ZERO)
                .alertThreshold(threshold)
                .alertTriggered(false)
                .enableThrottling(throttle)
                .periodStart(now)
                .periodEnd(calculatePeriodEnd(now, period))
                .lastUpdated(now)
                .build();
    }

    public static Instant calculatePeriodEnd(Instant start, BudgetPeriod period) {
        return switch (period) {
            case DAILY -> start.plus(1, ChronoUnit.DAYS);
            case WEEKLY -> start.plus(7, ChronoUnit.DAYS);
            case MONTHLY -> start.plus(30, ChronoUnit.DAYS);
        };
    }

    /**
     * Reset the budget if expired based on new start time
     */
    public void resetForNewPeriod(Instant newStartDate) {
        this.periodStart = newStartDate;
        this.periodEnd = calculatePeriodEnd(newStartDate, this.period);
        this.currentSpending = BigDecimal.ZERO;
        this.alertTriggered = false;
        this.lastUpdated = Instant.now();
    }

    /**
     * Check if budget is exceeded
     */
    public boolean isBudgetExceeded() {
        return currentSpending.compareTo(budgetLimit) >= 0;
    }

    /**
     * Check if alert threshold is reached
     */
    public boolean isAlertThresholdReached() {
        BigDecimal thresholdAmount = budgetLimit.multiply(
                new BigDecimal(alertThreshold).divide(new BigDecimal(100), 2, RoundingMode.HALF_UP));
        return currentSpending.compareTo(thresholdAmount) >= 0;
    }

    /**
     * Get remaining budget
     */
    public BigDecimal getRemainingBudget() {
        return budgetLimit.subtract(currentSpending);
    }

    /**
     * Get budget utilization percentage
     */
    public int getUtilizationPercentage() {
        if (budgetLimit.compareTo(BigDecimal.ZERO) == 0) {
            return 0;
        }
        return currentSpending.multiply(new BigDecimal(100))
                .divide(budgetLimit, 0, RoundingMode.HALF_UP)
                .intValue();
    }
}
