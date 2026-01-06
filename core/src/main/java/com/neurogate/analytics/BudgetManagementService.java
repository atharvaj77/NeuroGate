package com.neurogate.analytics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Budget Management & Alerts Service.
 * Tracks budgets, alerts on thresholds, and throttles when exceeded.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetManagementService {

    private final BudgetAlertRepository budgetAlertRepository;
    private final UsageRecordRepository usageRecordRepository;
    private final com.neurogate.ops.notification.NotificationService notificationService;

    /**
     * Create or update budget alert for a user
     *
     * @param userId           User identifier
     * @param budgetLimit      Budget limit in USD
     * @param period           Budget period (DAILY, WEEKLY, MONTHLY)
     * @param alertThreshold   Alert threshold percentage (e.g., 80)
     * @param enableThrottling Enable throttling when budget exceeded
     * @return Created budget alert
     */
    @Transactional
    public BudgetAlert createUserBudget(String userId, BigDecimal budgetLimit,
            BudgetAlert.BudgetPeriod period,
            int alertThreshold, boolean enableThrottling) {

        BudgetAlert alert = BudgetAlert.createForUser(userId, budgetLimit, period, alertThreshold, enableThrottling);
        return budgetAlertRepository.save(alert);
    }

    /**
     * Create or update budget alert for a team
     */
    @Transactional
    public BudgetAlert createTeamBudget(String teamId, BigDecimal budgetLimit,
            BudgetAlert.BudgetPeriod period,
            int alertThreshold, boolean enableThrottling) {

        BudgetAlert alert = BudgetAlert.createForTeam(teamId, budgetLimit, period, alertThreshold, enableThrottling);
        return budgetAlertRepository.save(alert);
    }

    /**
     * Update spending for a user/team
     * Called after each request
     */
    @Transactional
    public void updateSpending(String entityId, BudgetAlert.EntityType entityType, BigDecimal cost) {
        budgetAlertRepository.findActiveAlert(entityId, entityType, Instant.now())
                .ifPresent(alert -> {
                    alert.setCurrentSpending(alert.getCurrentSpending().add(cost));
                    alert.setLastUpdated(Instant.now());

                    // Check if alert should be triggered
                    if (!alert.getAlertTriggered() && alert.isAlertThresholdReached()) {
                        alert.setAlertTriggered(true);
                        sendAlert(alert);
                    }

                    budgetAlertRepository.save(alert);
                    log.debug("Updated spending for {}: ${} ({}% of budget)",
                            entityId, alert.getCurrentSpending(), alert.getUtilizationPercentage());
                });
    }

    /**
     * Check if request should be throttled due to budget
     *
     * @param userId User identifier
     * @return true if request should be throttled
     */
    public boolean shouldThrottle(String userId) {
        return budgetAlertRepository.findActiveAlert(userId, BudgetAlert.EntityType.USER, Instant.now())
                .map(alert -> alert.getEnableThrottling() && alert.isBudgetExceeded())
                .orElse(false);
    }

    /**
     * Check if team request should be throttled
     */
    public boolean shouldThrottleTeam(String teamId) {
        return budgetAlertRepository.findActiveAlert(teamId, BudgetAlert.EntityType.TEAM, Instant.now())
                .map(alert -> alert.getEnableThrottling() && alert.isBudgetExceeded())
                .orElse(false);
    }

    /**
     * Get budget status for a user
     */
    public BudgetStatus getBudgetStatus(String userId) {
        return budgetAlertRepository.findActiveAlert(userId, BudgetAlert.EntityType.USER, Instant.now())
                .map(alert -> BudgetStatus.builder()
                        .budgetLimit(alert.getBudgetLimit())
                        .currentSpending(alert.getCurrentSpending())
                        .remainingBudget(alert.getRemainingBudget())
                        .utilizationPercentage(alert.getUtilizationPercentage())
                        .budgetExceeded(alert.isBudgetExceeded())
                        .throttlingEnabled(alert.getEnableThrottling())
                        .periodEnd(alert.getPeriodEnd())
                        .build())
                .orElse(BudgetStatus.builder()
                        .budgetLimit(BigDecimal.ZERO)
                        .currentSpending(BigDecimal.ZERO)
                        .build());
    }

    /**
     * Send alert notification (email, Slack, etc.)
     */
    private void sendAlert(BudgetAlert alert) {
        log.warn("BUDGET ALERT: {} {} has reached {}% of budget (${} / ${})",
                alert.getEntityType(), alert.getEntityId(),
                alert.getUtilizationPercentage(),
                alert.getCurrentSpending(), alert.getBudgetLimit());

        notificationService.sendBudgetAlert(alert);
    }

    @Scheduled(fixedRate = 300000) // 5 minutes
    @Transactional
    public void checkPendingAlerts() {
        List<BudgetAlert> pendingAlerts = budgetAlertRepository.findPendingAlerts(Instant.now());

        for (BudgetAlert alert : pendingAlerts) {
            if (!alert.getAlertTriggered()) {
                alert.setAlertTriggered(true);
                sendAlert(alert);
                budgetAlertRepository.save(alert);
            }
        }

        if (!pendingAlerts.isEmpty()) {
            log.info("Triggered {} budget alerts", pendingAlerts.size());
        }
    }

    @Scheduled(fixedRate = 3600000) // 1 hour
    @Transactional
    public void resetExpiredBudgets() {
        List<BudgetAlert> expiredAlerts = budgetAlertRepository.findExpiredAlerts(Instant.now());

        for (BudgetAlert alert : expiredAlerts) {
            alert.resetForNewPeriod(Instant.now());
            budgetAlertRepository.save(alert);
        }

        if (!expiredAlerts.isEmpty()) {
            log.info("Reset {} expired budgets", expiredAlerts.size());
        }
    }

    /**
     * Budget status DTO
     */
    @lombok.Builder
    @lombok.Data
    public static class BudgetStatus {
        private BigDecimal budgetLimit;
        private BigDecimal currentSpending;
        private BigDecimal remainingBudget;
        private int utilizationPercentage;
        private boolean budgetExceeded;
        private boolean throttlingEnabled;
        private Instant periodEnd;
    }
}
