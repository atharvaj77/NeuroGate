package com.neurogate.analytics;

import com.neurogate.analytics.AnalyticsController.CostReport;
import com.neurogate.analytics.AnalyticsController.ExpensiveQuery;
import com.neurogate.analytics.AnalyticsController.TeamCostReport;
import com.neurogate.analytics.AnalyticsController.UserCostSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for analytics and reporting business logic.
 * Handles cost calculations, aggregations, and report generation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

        private final UsageRecordRepository usageRecordRepository;

        /**
         * Generate cost report for a user within a date range.
         */
        public CostReport getUserCostReport(String userId, LocalDate from, LocalDate to) {
                Instant start = from.atStartOfDay().toInstant(ZoneOffset.UTC);
                Instant end = to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

                // Get all usage records for user
                List<UsageRecord> records = usageRecordRepository.findByUserIdAndTimestampBetween(userId, start, end);

                BigDecimal totalCost = records.stream()
                                .map(UsageRecord::getCostUsd)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                Map<String, BigDecimal> providerBreakdown = records.stream()
                                .collect(Collectors.groupingBy(
                                                UsageRecord::getProvider,
                                                Collectors.reducing(BigDecimal.ZERO,
                                                                UsageRecord::getCostUsd,
                                                                BigDecimal::add)));

                Map<String, BigDecimal> modelBreakdown = records.stream()
                                .collect(Collectors.groupingBy(
                                                UsageRecord::getModel,
                                                Collectors.reducing(BigDecimal.ZERO,
                                                                UsageRecord::getCostUsd,
                                                                BigDecimal::add)));

                long totalRequests = records.size();

                long cacheHits = records.stream()
                                .filter(UsageRecord::getCacheHit)
                                .count();

                return CostReport.builder()
                                .userId(userId)
                                .period(from + " to " + to)
                                .totalCost(totalCost)
                                .totalRequests(totalRequests)
                                .cacheHits(cacheHits)
                                .cacheHitRate(totalRequests > 0 ? (double) cacheHits / totalRequests : 0.0)
                                .providerBreakdown(providerBreakdown)
                                .modelBreakdown(modelBreakdown)
                                .build();
        }

        /**
         * Generate cost report for a team within a date range.
         */
        public TeamCostReport getTeamCostReport(String teamId, LocalDate from, LocalDate to) {
                Instant start = from.atStartOfDay().toInstant(ZoneOffset.UTC);
                Instant end = to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

                // Get all usage records for team
                List<UsageRecord> records = usageRecordRepository.findByTeamIdAndTimestampBetween(teamId, start, end);

                // Calculate total cost
                BigDecimal totalCost = records.stream()
                                .map(UsageRecord::getCostUsd)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Get per-user breakdown
                Map<String, UserCostSummary> userBreakdown = records.stream()
                                .collect(Collectors.groupingBy(UsageRecord::getUserId))
                                .entrySet().stream()
                                .collect(Collectors.toMap(
                                                Map.Entry::getKey,
                                                entry -> {
                                                        List<UsageRecord> userRecords = entry.getValue();
                                                        BigDecimal userCost = userRecords.stream()
                                                                        .map(UsageRecord::getCostUsd)
                                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                                                        // Get top models for this user
                                                        List<String> topModels = userRecords.stream()
                                                                        .collect(Collectors.groupingBy(
                                                                                        UsageRecord::getModel,
                                                                                        Collectors.reducing(
                                                                                                        BigDecimal.ZERO,
                                                                                                        UsageRecord::getCostUsd,
                                                                                                        BigDecimal::add)))
                                                                        .entrySet().stream()
                                                                        .sorted(Map.Entry
                                                                                        .<String, BigDecimal>comparingByValue()
                                                                                        .reversed())
                                                                        .limit(3)
                                                                        .map(e -> e.getKey() + ": $" + e.getValue())
                                                                        .toList();

                                                        return UserCostSummary.builder()
                                                                        .cost(userCost)
                                                                        .requests(userRecords.size())
                                                                        .topModels(topModels)
                                                                        .build();
                                                }));

                return TeamCostReport.builder()
                                .teamId(teamId)
                                .period(from + " to " + to)
                                .totalCost(totalCost)
                                .userBreakdown(userBreakdown)
                                .build();
        }

        /**
         * Get top N expensive queries for a team.
         */
        public List<ExpensiveQuery> getTopExpensiveQueries(String teamId, int limit) {
                List<UsageRecord> topQueries = usageRecordRepository.findTopExpensiveQueries(teamId, limit);

                return topQueries.stream()
                                .map(record -> ExpensiveQuery.builder()
                                                .requestId(record.getRequestId())
                                                .userId(record.getUserId())
                                                .model(record.getModel())
                                                .cost(record.getCostUsd())
                                                .tokens(record.getTotalTokens())
                                                .timestamp(record.getTimestamp())
                                                .build())
                                .toList();
        }
}
