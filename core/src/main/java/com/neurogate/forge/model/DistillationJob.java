package com.neurogate.forge.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "distillation_jobs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistillationJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String jobId; // Provider-specific Job ID (e.g., ftjob-xyz)

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private JobStatus status;

    private int datasetSize;

    private String baseModel;

    private String resultingModelId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column
    private Map<String, Double> evalMetrics; // e.g., training_loss, validation_loss

    @Column(nullable = false)
    private Instant createdAt;

    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public enum JobStatus {
        COLLECTING,
        UPLOADING,
        TRAINING,
        COMPLETED,
        FAILED,
        DEPLOYING
    }
}
