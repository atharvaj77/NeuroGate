package com.neurogate.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/usage")
@RequiredArgsConstructor
@Tag(name = "Usage", description = "Organization and API key usage")
@RequiresRole(Role.VIEWER)
public class UsageController {

    private final UsageTracker usageTracker;

    @Operation(summary = "Get current usage", description = "Current monthly usage for the authenticated organization")
    @ApiResponse(responseCode = "200", description = "Usage retrieved")
    @GetMapping
    public ResponseEntity<UsageTracker.CurrentUsage> getCurrentUsage(Authentication authentication) {
        String orgId = SecurityUtils.resolveOrgId(authentication)
                .orElseThrow(() -> new IllegalArgumentException("Organization context is required"));
        return ResponseEntity.ok(usageTracker.getCurrentUsage(orgId));
    }

    @Operation(summary = "Get usage history", description = "Daily usage rollup for the authenticated organization")
    @ApiResponse(responseCode = "200", description = "History retrieved")
    @GetMapping("/history")
    public ResponseEntity<List<UsageTracker.DailyUsage>> getUsageHistory(
            Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        String orgId = SecurityUtils.resolveOrgId(authentication)
                .orElseThrow(() -> new IllegalArgumentException("Organization context is required"));
        return ResponseEntity.ok(usageTracker.getHistory(orgId, from, to));
    }

    @Operation(summary = "Get per-key usage", description = "Daily usage details for a specific API key")
    @ApiResponse(responseCode = "200", description = "Per-key usage retrieved")
    @GetMapping("/keys/{id}")
    public ResponseEntity<UsageTracker.KeyUsage> getKeyUsage(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(usageTracker.getKeyUsage(id, from, to));
    }
}
