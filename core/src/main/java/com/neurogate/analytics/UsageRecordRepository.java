package com.neurogate.analytics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for UsageRecord entities
 */
@Repository
public interface UsageRecordRepository extends JpaRepository<UsageRecord, UUID> {

    /**
     * Find all usage records for a user within a time range
     */
    List<UsageRecord> findByUserIdAndTimestampBetween(String userId, Instant start, Instant end);

    /**
     * Find all usage records for a team within a time range
     */
    List<UsageRecord> findByTeamIdAndTimestampBetween(String teamId, Instant start, Instant end);

    /**
     * Get total cost for a user within a time range
     */
    @Query("SELECT SUM(u.costUsd) FROM UsageRecord u WHERE u.userId = :userId AND u.timestamp BETWEEN :start AND :end")
    BigDecimal getTotalCostByUser(@Param("userId") String userId, @Param("start") Instant start, @Param("end") Instant end);

    /**
     * Get total cost for a team within a time range
     */
    @Query("SELECT SUM(u.costUsd) FROM UsageRecord u WHERE u.teamId = :teamId AND u.timestamp BETWEEN :start AND :end")
    BigDecimal getTotalCostByTeam(@Param("teamId") String teamId, @Param("start") Instant start, @Param("end") Instant end);

    /**
     * Get top N expensive queries for a team
     */
    @Query("SELECT u FROM UsageRecord u WHERE u.teamId = :teamId ORDER BY u.costUsd DESC LIMIT :limit")
    List<UsageRecord> findTopExpensiveQueries(@Param("teamId") String teamId, @Param("limit") int limit);

    /**
     * Get usage breakdown by provider for a user
     */
    @Query("SELECT u.provider, SUM(u.costUsd) FROM UsageRecord u WHERE u.userId = :userId AND u.timestamp BETWEEN :start AND :end GROUP BY u.provider")
    List<Object[]> getProviderBreakdownByUser(@Param("userId") String userId, @Param("start") Instant start, @Param("end") Instant end);
}
