package com.neurogate.analytics;

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
public class AnalyticsController {

        private final AnalyticsService analyticsService;

        @GetMapping("/costs/user/{userId}")
        public ResponseEntity<CostReport> getUserCosts(
                        @PathVariable String userId,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

                CostReport report = analyticsService.getUserCostReport(userId, from, to);
                return ResponseEntity.ok(report);
        }

        @GetMapping("/costs/team/{teamId}")
        public ResponseEntity<TeamCostReport> getTeamCosts(
                        @PathVariable String teamId,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

                TeamCostReport report = analyticsService.getTeamCostReport(teamId, from, to);
                return ResponseEntity.ok(report);
        }

        @GetMapping("/costs/team/{teamId}/top-queries")
        public ResponseEntity<List<ExpensiveQuery>> getTopExpensiveQueries(
                        @PathVariable String teamId,
                        @RequestParam(defaultValue = "10") int limit) {

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
