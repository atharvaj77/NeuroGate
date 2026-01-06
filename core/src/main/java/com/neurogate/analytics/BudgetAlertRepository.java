package com.neurogate.analytics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for BudgetAlert entities
 */
@Repository
public interface BudgetAlertRepository extends JpaRepository<BudgetAlert, UUID> {

    /**
     * Find active budget alert for entity in current period
     */
    @Query("SELECT b FROM BudgetAlert b WHERE b.entityId = :entityId AND b.entityType = :entityType " +
           "AND b.periodStart <= :now AND b.periodEnd >= :now")
    Optional<BudgetAlert> findActiveAlert(
            @Param("entityId") String entityId,
            @Param("entityType") BudgetAlert.EntityType entityType,
            @Param("now") Instant now
    );

    /**
     * Find all alerts that have reached threshold but not yet triggered
     */
    @Query("SELECT b FROM BudgetAlert b WHERE b.alertTriggered = false " +
           "AND b.currentSpending >= (b.budgetLimit * b.alertThreshold / 100) " +
           "AND b.periodStart <= :now AND b.periodEnd >= :now")
    List<BudgetAlert> findPendingAlerts(@Param("now") Instant now);

    /**
     * Find all alerts where budget is exceeded and throttling is enabled
     */
    @Query("SELECT b FROM BudgetAlert b WHERE b.enableThrottling = true " +
           "AND b.currentSpending >= b.budgetLimit " +
           "AND b.periodStart <= :now AND b.periodEnd >= :now")
    List<BudgetAlert> findThrottledAlerts(@Param("now") Instant now);

    /**
     * Find all expired alerts that need to be reset
     */
    @Query("SELECT b FROM BudgetAlert b WHERE b.periodEnd < :now")
    List<BudgetAlert> findExpiredAlerts(@Param("now") Instant now);
}
