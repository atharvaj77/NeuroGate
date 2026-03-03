package com.neurogate.alert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neurogate.pulse.PulseStreamService;
import com.neurogate.pulse.dto.MetricsSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * AlertService — evaluates user-defined {@link AlertRule}s every 15 seconds
 * against the live {@link MetricsSnapshot} from {@link PulseStreamService}.
 *
 * <h2>State Machine</h2>
 * 
 * <pre>
 *   OK  -->  FIRING  -->  RESOLVED  -->  OK
 *        (threshold      (threshold
 *         exceeded)       cleared)
 * </pre>
 *
 * <h2>Cooldown</h2>
 * A rule will not re-fire within its {@code cooldownMinutes} window to prevent
 * alert storms.
 *
 * <h2>Notification Channels</h2>
 * <ul>
 * <li>{@code LOG} — writes to the application log (always active)</li>
 * <li>{@code SLACK} — HTTP POST to Slack incoming webhook URL</li>
 * <li>{@code WEBHOOK} — generic HTTP POST to any URL</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRuleRepository ruleRepository;
    private final AlertHistoryRepository historyRepository;
    private final PulseStreamService pulseStreamService;
    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;

    // -----------------------------------------------------------------------
    // Scheduled evaluation
    // -----------------------------------------------------------------------

    @Scheduled(fixedRate = 15_000)
    @Transactional
    public void evaluateRules() {
        List<AlertRule> rules = ruleRepository.findByEnabledTrue();
        if (rules.isEmpty())
            return;

        MetricsSnapshot snapshot = pulseStreamService.getCurrentSnapshot();

        for (AlertRule rule : rules) {
            try {
                evaluateRule(rule, snapshot);
            } catch (Exception e) {
                log.error("Error evaluating alert rule '{}' ({}): {}", rule.getName(), rule.getId(), e.getMessage());
            }
        }
    }

    // -----------------------------------------------------------------------
    // Rule evaluation
    // -----------------------------------------------------------------------

    private void evaluateRule(AlertRule rule, MetricsSnapshot snapshot) {
        double metricValue = extractMetricValue(rule.getMetricType(), snapshot);
        boolean violated = rule.isViolated(metricValue);

        Optional<AlertHistory> activeFiring = historyRepository.findActiveFiringByRuleId(rule.getId());

        if (violated) {
            if (activeFiring.isEmpty() && !rule.isInCooldown()) {
                // Transition: OK → FIRING
                fire(rule, metricValue);
            }
        } else {
            if (activeFiring.isPresent()) {
                // Transition: FIRING → RESOLVED
                resolve(activeFiring.get(), rule);
            }
        }
    }

    private double extractMetricValue(AlertRule.MetricType type, MetricsSnapshot snap) {
        return switch (type) {
            case LATENCY -> snap.getAvgLatencyMs();
            case P95_LATENCY -> snap.getP95LatencyMs();
            case ERROR_RATE -> snap.getErrorRate();
            case COST -> snap.getCostUsdTotal();
            case CACHE_HIT_RATE -> snap.getCacheHitRate();
            case RPS -> snap.getRps();
            case PROVIDER_HEALTH -> snap.getErrorRate(); // proxy
        };
    }

    // -----------------------------------------------------------------------
    // State transitions
    // -----------------------------------------------------------------------

    private void fire(AlertRule rule, double metricValue) {
        String msg = String.format("ALERT FIRING — rule='%s' metric=%s value=%.4f %s threshold=%.4f",
                rule.getName(), rule.getMetricType(), metricValue,
                rule.getOperator(), rule.getThreshold());

        log.warn(msg);

        AlertHistory history = AlertHistory.builder()
                .ruleId(rule.getId())
                .status(AlertHistory.AlertStatus.FIRING)
                .metricValue(metricValue)
                .message(msg)
                .firedAt(Instant.now())
                .build();

        historyRepository.save(history);

        rule.setLastFiredAt(Instant.now());
        ruleRepository.save(rule);

        sendNotification(rule, msg, metricValue);
    }

    private void resolve(AlertHistory firing, AlertRule rule) {
        firing.setResolvedAt(Instant.now());
        firing.setStatus(AlertHistory.AlertStatus.RESOLVED);
        historyRepository.save(firing);

        String msg = String.format("ALERT RESOLVED — rule='%s' metric=%s",
                rule.getName(), rule.getMetricType());
        log.info(msg);
    }

    // -----------------------------------------------------------------------
    // Manual CRUD helpers
    // -----------------------------------------------------------------------

    @Transactional
    public AlertRule createRule(AlertRule rule) {
        return ruleRepository.save(rule);
    }

    @Transactional
    public Optional<AlertRule> updateRule(UUID id, AlertRule patch) {
        return ruleRepository.findById(id).map(existing -> {
            existing.setName(patch.getName());
            existing.setMetricType(patch.getMetricType());
            existing.setOperator(patch.getOperator());
            existing.setThreshold(patch.getThreshold());
            existing.setDurationSeconds(patch.getDurationSeconds());
            existing.setCooldownMinutes(patch.getCooldownMinutes());
            existing.setChannel(patch.getChannel());
            existing.setChannelConfig(patch.getChannelConfig());
            existing.setEnabled(patch.isEnabled());
            return ruleRepository.save(existing);
        });
    }

    @Transactional
    public void deleteRule(UUID id) {
        ruleRepository.deleteById(id);
    }

    public List<AlertRule> listRules() {
        return ruleRepository.findAll();
    }

    public List<AlertHistory> getHistory(int limit) {
        return historyRepository.findTop50ByOrderByFiredAtDesc();
    }

    /**
     * Manually fire a test alert for a rule (does NOT count toward cooldown).
     */
    @Transactional
    public AlertHistory testRule(UUID ruleId) {
        AlertRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Rule not found: " + ruleId));

        String msg = String.format("TEST ALERT — rule='%s' metric=%s threshold=%.4f",
                rule.getName(), rule.getMetricType(), rule.getThreshold());
        log.info(msg);

        AlertHistory history = AlertHistory.builder()
                .ruleId(ruleId)
                .status(AlertHistory.AlertStatus.TEST)
                .message(msg)
                .firedAt(Instant.now())
                .build();

        historyRepository.save(history);
        sendNotification(rule, msg, rule.getThreshold());
        return history;
    }

    // -----------------------------------------------------------------------
    // Notification dispatch
    // -----------------------------------------------------------------------

    private void sendNotification(AlertRule rule, String message, double metricValue) {
        switch (rule.getChannel()) {
            case LOG -> {
                // Already logged above
            }
            case SLACK -> sendSlackNotification(rule, message, metricValue);
            case WEBHOOK -> sendWebhookNotification(rule, message, metricValue);
        }
    }

    private void sendSlackNotification(AlertRule rule, String message, double metricValue) {
        try {
            Map<?, ?> config = objectMapper.readValue(rule.getChannelConfig(), Map.class);
            String webhookUrl = (String) config.get("webhookUrl");
            if (webhookUrl == null || webhookUrl.isBlank()) {
                log.warn("Slack webhook URL not configured for rule '{}'", rule.getName());
                return;
            }

            String emoji = rule.getMetricType() == AlertRule.MetricType.ERROR_RATE ? ":rotating_light:" : ":warning:";
            Map<String, String> payload = Map.of(
                    "text",
                    String.format("%s *NeuroGate Alert* — %s\n> Metric: `%s` | Value: `%.4f` | Threshold: `%.4f`",
                            emoji, rule.getName(), rule.getMetricType(), metricValue, rule.getThreshold()));

            webClientBuilder.build()
                    .post()
                    .uri(webhookUrl)
                    .bodyValue(payload)
                    .retrieve()
                    .toBodilessEntity()
                    .subscribe(
                            res -> log.info("Slack notification sent for rule '{}'", rule.getName()),
                            err -> log.error("Failed to send Slack notification for rule '{}': {}", rule.getName(),
                                    err.getMessage()));
        } catch (Exception e) {
            log.error("Failed to parse Slack config for rule '{}'", rule.getName(), e);
        }
    }

    private void sendWebhookNotification(AlertRule rule, String message, double metricValue) {
        try {
            Map<?, ?> config = objectMapper.readValue(rule.getChannelConfig(), Map.class);
            String url = (String) config.get("url");
            if (url == null || url.isBlank())
                return;

            Map<String, Object> payload = Map.of(
                    "rule", rule.getName(),
                    "metric", rule.getMetricType().name(),
                    "value", metricValue,
                    "threshold", rule.getThreshold(),
                    "message", message,
                    "firedAt", Instant.now().toString());

            webClientBuilder.build()
                    .post()
                    .uri(url)
                    .bodyValue(payload)
                    .retrieve()
                    .toBodilessEntity()
                    .subscribe(
                            res -> log.info("Webhook notification sent for rule '{}'", rule.getName()),
                            err -> log.error("Failed to send webhook for rule '{}': {}", rule.getName(),
                                    err.getMessage()));
        } catch (Exception e) {
            log.error("Failed to parse webhook config for rule '{}'", rule.getName(), e);
        }
    }
}
