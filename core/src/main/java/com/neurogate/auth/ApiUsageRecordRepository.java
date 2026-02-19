package com.neurogate.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiUsageRecordRepository extends JpaRepository<ApiUsageRecord, UUID> {
    Optional<ApiUsageRecord> findByOrgIdAndApiKeyIdAndUsageDate(String orgId, UUID apiKeyId, LocalDate usageDate);

    List<ApiUsageRecord> findByOrgIdAndUsageDateBetweenOrderByUsageDateAsc(String orgId, LocalDate from, LocalDate to);

    List<ApiUsageRecord> findByApiKeyIdAndUsageDateBetweenOrderByUsageDateAsc(UUID apiKeyId, LocalDate from,
            LocalDate to);

    @Query("SELECT COALESCE(SUM(r.requestCount), 0) FROM ApiUsageRecord r WHERE r.orgId = :orgId AND r.usageDate BETWEEN :from AND :to")
    Long getRequestCountForOrgBetween(@Param("orgId") String orgId, @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(r.requestCount), 0) FROM ApiUsageRecord r WHERE r.apiKeyId = :apiKeyId AND r.usageDate BETWEEN :from AND :to")
    Long getRequestCountForKeyBetween(@Param("apiKeyId") UUID apiKeyId, @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
