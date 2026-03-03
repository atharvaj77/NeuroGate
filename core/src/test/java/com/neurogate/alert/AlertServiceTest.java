package com.neurogate.alert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neurogate.pulse.PulseStreamService;
import com.neurogate.pulse.dto.MetricsSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AlertService} rule evaluation, state transitions,
 * and cooldown logic.
 */
class AlertServiceTest {

    private AlertRuleRepository ruleRepository;
    private AlertHistoryRepository historyRepository;
    private PulseStreamService pulseStreamService;
    private AlertService alertService;

    @BeforeEach
    void setUp() {
        ruleRepository = mock(AlertRuleRepository.class);
        historyRepository = mock(AlertHistoryRepository.class);
        pulseStreamService = mock(PulseStreamService.class);

        alertService = new AlertService(
                ruleRepository, historyRepository,
                pulseStreamService,
                new ObjectMapper(),
                WebClient.builder());
    }

    @Test
    void ruleViolated_whenNoCooldown_shouldFire() {
        AlertRule rule = AlertRule.builder()
                .id(UUID.randomUUID())
                .name("High Latency")
                .metricType(AlertRule.MetricType.LATENCY)
                .operator(AlertRule.Operator.GT)
                .threshold(100.0)
                .cooldownMinutes(15)
                .enabled(true)
                .build();

        when(ruleRepository.findByEnabledTrue()).thenReturn(List.of(rule));
        when(pulseStreamService.getCurrentSnapshot()).thenReturn(
                MetricsSnapshot.builder()
                        .timestamp(Instant.now())
                        .avgLatencyMs(250.0) // > 100ms threshold
                        .p95LatencyMs(400.0)
                        .rps(10.0)
                        .errorRate(0.01)
                        .cacheHitRate(0.3)
                        .tokenCount(500L)
                        .piiBlocked(0L)
                        .activeProvider("openai")
                        .costUsdTotal(0.05)
                        .build());
        when(historyRepository.findActiveFiringByRuleId(rule.getId())).thenReturn(Optional.empty());
        when(historyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        alertService.evaluateRules();

        ArgumentCaptor<AlertHistory> captor = ArgumentCaptor.forClass(AlertHistory.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(AlertHistory.AlertStatus.FIRING);
    }

    @Test
    void ruleResolved_whenThresholdClears() {
        UUID ruleId = UUID.randomUUID();
        AlertRule rule = AlertRule.builder()
                .id(ruleId)
                .name("High Latency")
                .metricType(AlertRule.MetricType.LATENCY)
                .operator(AlertRule.Operator.GT)
                .threshold(100.0)
                .cooldownMinutes(15)
                .enabled(true)
                .build();

        AlertHistory firing = AlertHistory.builder()
                .id(UUID.randomUUID())
                .ruleId(ruleId)
                .status(AlertHistory.AlertStatus.FIRING)
                .build();

        when(ruleRepository.findByEnabledTrue()).thenReturn(List.of(rule));
        when(pulseStreamService.getCurrentSnapshot()).thenReturn(
                MetricsSnapshot.builder()
                        .timestamp(Instant.now())
                        .avgLatencyMs(50.0) // below threshold
                        .build());
        when(historyRepository.findActiveFiringByRuleId(ruleId)).thenReturn(Optional.of(firing));
        when(historyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        alertService.evaluateRules();

        ArgumentCaptor<AlertHistory> captor = ArgumentCaptor.forClass(AlertHistory.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(AlertHistory.AlertStatus.RESOLVED);
        assertThat(captor.getValue().getResolvedAt()).isNotNull();
    }

    @Test
    void cooldown_preventsRefiring() {
        AlertRule rule = AlertRule.builder()
                .id(UUID.randomUUID())
                .name("Cost Alert")
                .metricType(AlertRule.MetricType.COST)
                .operator(AlertRule.Operator.GT)
                .threshold(10.0)
                .cooldownMinutes(15)
                .enabled(true)
                // Set lastFiredAt to just now → within cooldown
                .lastFiredAt(Instant.now().minusSeconds(60))
                .build();

        when(ruleRepository.findByEnabledTrue()).thenReturn(List.of(rule));
        when(pulseStreamService.getCurrentSnapshot()).thenReturn(
                MetricsSnapshot.builder()
                        .timestamp(Instant.now())
                        .costUsdTotal(50.0) // way above threshold
                        .build());
        when(historyRepository.findActiveFiringByRuleId(rule.getId())).thenReturn(Optional.empty());

        alertService.evaluateRules();

        verify(historyRepository, never()).save(any());
    }
}
