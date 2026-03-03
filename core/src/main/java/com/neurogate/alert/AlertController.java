package com.neurogate.alert;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * AlertController — REST API for managing alert rules and viewing alert
 * history.
 *
 * <p>
 * <ul>
 * <li>{@code POST   /api/v1/alerts/rules} — create rule</li>
 * <li>{@code GET    /api/v1/alerts/rules} — list all rules</li>
 * <li>{@code PUT    /api/v1/alerts/rules/{id}} — update rule</li>
 * <li>{@code DELETE /api/v1/alerts/rules/{id}} — delete rule</li>
 * <li>{@code POST   /api/v1/alerts/rules/{id}/test} — send test alert</li>
 * <li>{@code GET    /api/v1/alerts/history} — recent alert events</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
@Tag(name = "Alerts", description = "Alert rule management and history")
public class AlertController {

    private final AlertService alertService;

    // -----------------------------------------------------------------------
    // Rules CRUD
    // -----------------------------------------------------------------------

    @Operation(summary = "Create an alert rule")
    @PostMapping("/rules")
    public ResponseEntity<AlertRule> createRule(@RequestBody AlertRule rule) {
        AlertRule saved = alertService.createRule(rule);
        return ResponseEntity.created(URI.create("/api/v1/alerts/rules/" + saved.getId())).body(saved);
    }

    @Operation(summary = "List all alert rules")
    @GetMapping("/rules")
    public ResponseEntity<List<AlertRule>> listRules() {
        return ResponseEntity.ok(alertService.listRules());
    }

    @Operation(summary = "Update an alert rule")
    @PutMapping("/rules/{id}")
    public ResponseEntity<AlertRule> updateRule(
            @Parameter(description = "Rule UUID") @PathVariable UUID id,
            @RequestBody AlertRule patch) {

        return alertService.updateRule(id, patch)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Delete an alert rule")
    @DeleteMapping("/rules/{id}")
    public ResponseEntity<Void> deleteRule(
            @Parameter(description = "Rule UUID") @PathVariable UUID id) {

        alertService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }

    // -----------------------------------------------------------------------
    // Test alert
    // -----------------------------------------------------------------------

    @Operation(summary = "Test an alert rule", description = "Manually triggers a test notification without affecting cooldown or state.")
    @PostMapping("/rules/{id}/test")
    public ResponseEntity<AlertHistory> testRule(
            @Parameter(description = "Rule UUID") @PathVariable UUID id) {

        AlertHistory result = alertService.testRule(id);
        return ResponseEntity.ok(result);
    }

    // -----------------------------------------------------------------------
    // History
    // -----------------------------------------------------------------------

    @Operation(summary = "Get recent alert history (last 50 events)")
    @GetMapping("/history")
    public ResponseEntity<List<AlertHistory>> getHistory(
            @Parameter(description = "Maximum events to return (default 50)") @RequestParam(defaultValue = "50") int limit) {

        return ResponseEntity.ok(alertService.getHistory(limit));
    }
}
