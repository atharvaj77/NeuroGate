package com.neurogate.alert;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AlertHistoryRepository extends JpaRepository<AlertHistory, UUID> {

    List<AlertHistory> findByRuleIdOrderByFiredAtDesc(UUID ruleId);

    List<AlertHistory> findTop50ByOrderByFiredAtDesc();

    @Query("SELECT a FROM AlertHistory a WHERE a.ruleId = :ruleId AND a.status = 'FIRING' AND a.resolvedAt IS NULL")
    Optional<AlertHistory> findActiveFiringByRuleId(@Param("ruleId") UUID ruleId);
}
