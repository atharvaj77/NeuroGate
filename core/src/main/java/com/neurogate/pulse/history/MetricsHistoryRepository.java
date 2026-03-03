package com.neurogate.pulse.history;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for {@link MetricsHistory} time-series records.
 *
 * <p>
 * All queries are ordered by {@code bucketTime} ascending so callers receive
 * chronological data suitable for chart rendering.
 */
@Repository
public interface MetricsHistoryRepository extends JpaRepository<MetricsHistory, Long> {

    /**
     * Fetch all one-minute buckets within [from, to].
     */
    List<MetricsHistory> findByBucketTimeBetweenOrderByBucketTimeAsc(Instant from, Instant to);

    /**
     * Compute hourly aggregates for a time range.
     * Returns rows: [bucketHour, avgRps, avgLatency, p95Latency, avgErrorRate,
     * sumTokens, avgCacheHitRate, sumCost, sumPiiBlocked]
     */
    @Query("""
            SELECT
              date_trunc('hour', m.bucketTime)        AS hour,
              AVG(m.rps)                              AS avgRps,
              AVG(m.avgLatencyMs)                     AS avgLatency,
              AVG(m.p95LatencyMs)                     AS avgP95,
              AVG(m.errorRate)                        AS avgErrorRate,
              SUM(m.tokenCount)                       AS sumTokens,
              AVG(m.cacheHitRate)                     AS avgCacheHitRate,
              SUM(m.costUsd)                          AS sumCost,
              SUM(m.piiBlocked)                       AS sumPii
            FROM MetricsHistory m
            WHERE m.bucketTime BETWEEN :from AND :to
            GROUP BY date_trunc('hour', m.bucketTime)
            ORDER BY hour ASC
            """)
    List<Object[]> findHourlyAggregates(@Param("from") Instant from, @Param("to") Instant to);

    /**
     * Compute daily aggregates for a time range.
     */
    @Query("""
            SELECT
              date_trunc('day', m.bucketTime)         AS day,
              AVG(m.rps)                              AS avgRps,
              AVG(m.avgLatencyMs)                     AS avgLatency,
              AVG(m.p95LatencyMs)                     AS avgP95,
              AVG(m.errorRate)                        AS avgErrorRate,
              SUM(m.tokenCount)                       AS sumTokens,
              AVG(m.cacheHitRate)                     AS avgCacheHitRate,
              SUM(m.costUsd)                          AS sumCost,
              SUM(m.piiBlocked)                       AS sumPii
            FROM MetricsHistory m
            WHERE m.bucketTime BETWEEN :from AND :to
            GROUP BY date_trunc('day', m.bucketTime)
            ORDER BY day ASC
            """)
    List<Object[]> findDailyAggregates(@Param("from") Instant from, @Param("to") Instant to);

    /**
     * Summary stats for a window: averages and sums.
     */
    @Query("""
            SELECT
              COUNT(m),
              AVG(m.rps),
              AVG(m.avgLatencyMs),
              AVG(m.p95LatencyMs),
              AVG(m.errorRate),
              SUM(m.tokenCount),
              AVG(m.cacheHitRate),
              SUM(m.costUsd),
              SUM(m.piiBlocked)
            FROM MetricsHistory m
            WHERE m.bucketTime >= :since
            """)
    Object[] getSummaryStats(@Param("since") Instant since);
}
