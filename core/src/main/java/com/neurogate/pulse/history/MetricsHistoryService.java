package com.neurogate.pulse.history;

import com.neurogate.pulse.PulseStreamService;
import com.neurogate.pulse.dto.MetricsSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MetricsHistoryService — persists one-minute {@link MetricsHistory} snapshots
 * and serves historical queries with configurable granularity.
 *
 * <p>
 * The {@code snapshotMetrics()} method is called every minute by a
 * {@link Scheduled} task. Query methods support "minute", "hour", and "day"
 * granularity, automatically rebucketing stored data.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsHistoryService {

    private final MetricsHistoryRepository repository;
    private final PulseStreamService pulseStreamService;

    // -----------------------------------------------------------------------
    // Scheduled snapshot
    // -----------------------------------------------------------------------

    /**
     * Captures the current rolling-window snapshot and stores it as a
     * one-minute bucket in {@code metrics_history}.
     */
    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void snapshotMetrics() {
        try {
            MetricsSnapshot snap = pulseStreamService.getCurrentSnapshot();
            Instant bucket = Instant.now().truncatedTo(ChronoUnit.MINUTES);

            MetricsHistory record = MetricsHistory.builder()
                    .bucketTime(bucket)
                    .rps(snap.getRps())
                    .avgLatencyMs(snap.getAvgLatencyMs())
                    .p95LatencyMs(snap.getP95LatencyMs())
                    .errorRate(snap.getErrorRate())
                    .tokenCount(snap.getTokenCount())
                    .cacheHitRate(snap.getCacheHitRate())
                    .costUsd(snap.getCostUsdTotal())
                    .piiBlocked(snap.getPiiBlocked())
                    .provider(snap.getActiveProvider())
                    .build();

            repository.save(record);
            log.debug("Snapshot saved for bucket {}", bucket);
        } catch (Exception e) {
            log.error("Failed to snapshot metrics", e);
        }
    }

    // -----------------------------------------------------------------------
    // Query methods
    // -----------------------------------------------------------------------

    /**
     * Returns time-series data between {@code from} and {@code to} at the
     * requested {@code granularity} ("minute", "hour", "day").
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getHistory(Instant from, Instant to, String granularity) {
        return switch (granularity.toLowerCase()) {
            case "hour" -> mapHourly(repository.findHourlyAggregates(from, to));
            case "day" -> mapDaily(repository.findDailyAggregates(from, to));
            default -> mapMinute(repository.findByBucketTimeBetweenOrderByBucketTimeAsc(from, to));
        };
    }

    /**
     * Returns a summary object for the last {@code period} (e.g. "1h", "24h", "7d",
     * "30d").
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getSummary(String period) {
        Instant since = parsePeriod(period);
        Object[] stats = repository.getSummaryStats(since);

        if (stats == null || stats[0] == null) {
            return Map.of("period", period, "buckets", 0);
        }

        return Map.of(
                "period", period,
                "buckets", stats[0],
                "avgRps", orZero(stats[1]),
                "avgLatencyMs", orZero(stats[2]),
                "avgP95Ms", orZero(stats[3]),
                "avgErrorRate", orZero(stats[4]),
                "totalTokens", orZero(stats[5]),
                "avgCacheHitRate", orZero(stats[6]),
                "totalCostUsd", orZero(stats[7]),
                "totalPiiBlocked", orZero(stats[8]));
    }

    /**
     * Returns per-provider cost and request breakdown for the last 24 hours.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getProviderBreakdown() {
        Instant from = Instant.now().minus(Duration.ofHours(24));
        List<MetricsHistory> records = repository.findByBucketTimeBetweenOrderByBucketTimeAsc(from, Instant.now());

        Map<String, Double> costByProvider = records.stream()
                .filter(r -> r.getProvider() != null)
                .collect(Collectors.groupingBy(MetricsHistory::getProvider,
                        Collectors.summingDouble(MetricsHistory::getCostUsd)));

        Map<String, Long> countByProvider = records.stream()
                .filter(r -> r.getProvider() != null)
                .collect(Collectors.groupingBy(MetricsHistory::getProvider,
                        Collectors.counting()));

        return Map.of(
                "costByProvider", costByProvider,
                "countByProvider", countByProvider);
    }

    // -----------------------------------------------------------------------
    // Mapping helpers
    // -----------------------------------------------------------------------

    private List<Map<String, Object>> mapMinute(List<MetricsHistory> rows) {
        List<Map<String, Object>> result = new ArrayList<>(rows.size());
        for (MetricsHistory r : rows) {
            result.add(Map.of(
                    "time", r.getBucketTime(),
                    "rps", r.getRps(),
                    "avgLatencyMs", r.getAvgLatencyMs(),
                    "p95LatencyMs", r.getP95LatencyMs(),
                    "errorRate", r.getErrorRate(),
                    "tokenCount", r.getTokenCount(),
                    "cacheHitRate", r.getCacheHitRate(),
                    "costUsd", r.getCostUsd(),
                    "piiBlocked", r.getPiiBlocked()));
        }
        return result;
    }

    private List<Map<String, Object>> mapHourly(List<Object[]> rows) {
        return mapAggregated(rows);
    }

    private List<Map<String, Object>> mapDaily(List<Object[]> rows) {
        return mapAggregated(rows);
    }

    private List<Map<String, Object>> mapAggregated(List<Object[]> rows) {
        List<Map<String, Object>> result = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            result.add(Map.of(
                    "time", r[0],
                    "avgRps", orZero(r[1]),
                    "avgLatencyMs", orZero(r[2]),
                    "avgP95Ms", orZero(r[3]),
                    "avgErrorRate", orZero(r[4]),
                    "totalTokens", orZero(r[5]),
                    "cacheHitRate", orZero(r[6]),
                    "totalCostUsd", orZero(r[7]),
                    "totalPiiBlocked", orZero(r[8])));
        }
        return result;
    }

    // -----------------------------------------------------------------------
    // Utility
    // -----------------------------------------------------------------------

    private Instant parsePeriod(String period) {
        return switch (period.toLowerCase()) {
            case "1h" -> Instant.now().minus(Duration.ofHours(1));
            case "6h" -> Instant.now().minus(Duration.ofHours(6));
            case "7d" -> Instant.now().minus(Duration.ofDays(7));
            case "30d" -> Instant.now().minus(Duration.ofDays(30));
            default -> Instant.now().minus(Duration.ofHours(24)); // 24h default
        };
    }

    private Object orZero(Object val) {
        return val != null ? val : 0;
    }
}
