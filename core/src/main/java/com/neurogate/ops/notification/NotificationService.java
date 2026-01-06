package com.neurogate.ops.notification;

import com.neurogate.analytics.BudgetAlert;

public interface NotificationService {
    void sendBudgetAlert(BudgetAlert alert);
}
