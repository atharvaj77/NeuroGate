package com.neurogate.analytics;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Cost Analytics API
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Usage analytics and cost tracking")
public class AnalyticsController {

        private final AnalyticsService analyticsService;

        @Operation(summary = "Get user cost report", description = "Retrieve cost breakdown for a specific user over a date range")
        @ApiResponse(responseCode = "200", description = "Cost report retrieved successfully")
        @GetMapping("/costs/user/{userId}")
        public ResponseEntity<CostReport> getUserCosts(
                        @Parameter(description = "User ID") @PathVariable String userId,
                        @Parameter(description = "Start date (ISO format)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                        @Parameter(description = "End date (ISO format)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

                CostReport report = analyticsService.getUserCostReport(userId, from, to);
                return ResponseEntity.ok(report);
        }

        @Operation(summary = "Get team cost report", description = "Retrieve cost breakdown for a team with user-level details")
        @ApiResponse(responseCode = "200", description = "Team cost report retrieved successfully")
        @GetMapping("/costs/team/{teamId}")
        public ResponseEntity<TeamCostReport> getTeamCosts(
                        @Parameter(description = "Team ID") @PathVariable String teamId,
                        @Parameter(description = "Start date (ISO format)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                        @Parameter(description = "End date (ISO format)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

                TeamCostReport report = analyticsService.getTeamCostReport(teamId, from, to);
                return ResponseEntity.ok(report);
        }

        @Operation(summary = "Get top expensive queries", description = "Retrieve the most expensive queries for a team")
        @ApiResponse(responseCode = "200", description = "Expensive queries retrieved successfully")
        @GetMapping("/costs/team/{teamId}/top-queries")
        public ResponseEntity<List<ExpensiveQuery>> getTopExpensiveQueries(
                        @Parameter(description = "Team ID") @PathVariable String teamId,
                        @Parameter(description = "Maximum number of queries to return") @RequestParam(defaultValue = "10") int limit) {

                List<ExpensiveQuery> result = analyticsService.getTopExpensiveQueries(teamId, limit);
                return ResponseEntity.ok(result);
        }

        @Data
        @Builder
        public static class CostReport {
                private String userId;
                private String period;
                private BigDecimal totalCost;
                private long totalRequests;
                private long cacheHits;
                private double cacheHitRate;
                private Map<String, BigDecimal> providerBreakdown;
                private Map<String, BigDecimal> modelBreakdown;
        }

        @Data
        @Builder
        public static class TeamCostReport {
                private String teamId;
                private String period;
                private BigDecimal totalCost;
                private Map<String, UserCostSummary> userBreakdown;
        }

        @Data
        @Builder
        public static class UserCostSummary {
                private BigDecimal cost;
                private long requests;
                private List<String> topModels;
        }

        @Data
        @Builder
        public static class ExpensiveQuery {
                private String requestId;
                private String userId;
                private String model;
                private BigDecimal cost;
                private Integer tokens;
                private Instant timestamp;
        }
}
