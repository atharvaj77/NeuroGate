package com.neurogate.analytics;

import com.neurogate.ops.notification.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = com.neurogate.NeuroGateApplication.class)
@ActiveProfiles("test")
class BudgetManagementServiceTest {

    @Autowired
    private BudgetManagementService budgetManagementService;

    @MockBean
    private BudgetAlertRepository budgetAlertRepository;

    @MockBean
    private UsageRecordRepository usageRecordRepository;

    @MockBean
    private NotificationService notificationService;

    @Test
    void testCreateUserBudget() {
        when(budgetAlertRepository.save(any(BudgetAlert.class))).thenAnswer(i -> i.getArguments()[0]);

        BudgetAlert alert = budgetManagementService.createUserBudget(
                "user-1",
                new BigDecimal("100.00"),
                BudgetAlert.BudgetPeriod.MONTHLY,
                80,
                true);

        assertNotNull(alert);
        assertEquals("user-1", alert.getEntityId());
        assertEquals(new BigDecimal("100.00"), alert.getBudgetLimit());
        assertEquals(BudgetAlert.EntityType.USER, alert.getEntityType());
        verify(budgetAlertRepository).save(any(BudgetAlert.class));
    }

    @Test
    void testUpdateSpending_TriggersAlert() {
        // Mock existing alert
        BudgetAlert mockAlert = BudgetAlert.createForUser(
                "user-1", new BigDecimal("100.00"), BudgetAlert.BudgetPeriod.MONTHLY, 80, true);
        // Current spending 70
        mockAlert.setCurrentSpending(new BigDecimal("70.00"));

        when(budgetAlertRepository.findActiveAlert(eq("user-1"), eq(BudgetAlert.EntityType.USER), any(Instant.class)))
                .thenReturn(Optional.of(mockAlert));

        when(budgetAlertRepository.save(any(BudgetAlert.class))).thenAnswer(i -> i.getArguments()[0]);

        // Update spending by 15 -> Total 85 (85% > 80%)
        budgetManagementService.updateSpending("user-1", BudgetAlert.EntityType.USER, new BigDecimal("15.00"));

        // Verify alert triggered
        assertTrue(mockAlert.getAlertTriggered());
        verify(notificationService, times(1)).sendBudgetAlert(mockAlert);
        verify(budgetAlertRepository).save(mockAlert);
    }

    @Test
    void testUpdateSpending_NoAlertWhenUnderThreshold() {
        BudgetAlert mockAlert = BudgetAlert.createForUser(
                "user-1", new BigDecimal("100.00"), BudgetAlert.BudgetPeriod.MONTHLY, 80, true);
        mockAlert.setCurrentSpending(new BigDecimal("10.00"));

        when(budgetAlertRepository.findActiveAlert(eq("user-1"), eq(BudgetAlert.EntityType.USER), any(Instant.class)))
                .thenReturn(Optional.of(mockAlert));

        // Update by 10 -> Total 20 (20%)
        budgetManagementService.updateSpending("user-1", BudgetAlert.EntityType.USER, new BigDecimal("10.00"));

        assertFalse(mockAlert.getAlertTriggered());
        verify(notificationService, never()).sendBudgetAlert(any());
        verify(budgetAlertRepository).save(mockAlert);
    }

    @Test
    void testShouldThrottle() {
        BudgetAlert mockAlert = BudgetAlert.createForUser(
                "user-1", new BigDecimal("100.00"), BudgetAlert.BudgetPeriod.MONTHLY, 80, true);
        mockAlert.setCurrentSpending(new BigDecimal("101.00")); // Exceeded

        when(budgetAlertRepository.findActiveAlert(eq("user-1"), eq(BudgetAlert.EntityType.USER), any(Instant.class)))
                .thenReturn(Optional.of(mockAlert));

        boolean throttle = budgetManagementService.shouldThrottle("user-1");
        assertTrue(throttle);
    }
}
