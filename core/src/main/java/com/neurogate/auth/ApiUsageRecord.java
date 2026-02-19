package com.neurogate.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "api_usage_records", uniqueConstraints = @UniqueConstraint(name = "uk_api_usage_org_key_day", columnNames = {
        "org_id", "api_key_id", "usage_date"
}), indexes = {
        @Index(name = "idx_api_usage_org_day", columnList = "org_id,usage_date"),
        @Index(name = "idx_api_usage_key_day", columnList = "api_key_id,usage_date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiUsageRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false, length = 64)
    private String orgId;

    @Column(name = "api_key_id", nullable = false)
    private UUID apiKeyId;

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    @Column(name = "request_count", nullable = false)
    private Long requestCount;

    @Column(name = "token_count", nullable = false)
    private Long tokenCount;

    @Column(name = "cost_usd", nullable = false, precision = 12, scale = 6)
    private BigDecimal costUsd;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (requestCount == null) {
            requestCount = 0L;
        }
        if (tokenCount == null) {
            tokenCount = 0L;
        }
        if (costUsd == null) {
            costUsd = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
