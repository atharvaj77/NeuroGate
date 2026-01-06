package com.neurogate.ops.notification;

import com.neurogate.analytics.BudgetAlert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ConsoleNotificationService implements NotificationService {

    @Override
    public void sendBudgetAlert(BudgetAlert alert) {
        // Log alert to console (default channel)
        log.warn("==================================================");
        log.warn("NOTIFICATION: BUDGET ALERT TRIGGERED");
        log.warn("Entity: {} ({})", alert.getEntityId(), alert.getEntityType());
        log.warn("Usage: {}% (${} / ${})",
                alert.getUtilizationPercentage(),
                alert.getCurrentSpending(),
                alert.getBudgetLimit());
        log.warn("==================================================");
    }
}
