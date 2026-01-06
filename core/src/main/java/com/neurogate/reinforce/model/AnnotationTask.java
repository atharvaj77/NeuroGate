package com.neurogate.reinforce.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "annotation_tasks")
public class AnnotationTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trace_id", nullable = false)
    private String traceId;

    @Column(columnDefinition = "TEXT")
    private String input;

    @Column(columnDefinition = "TEXT")
    private String output;

    @Column(name = "sampler_source")
    private String samplerSource; // "RANDOM", "LOW_CONFIDENCE", "USER_FLAG"

    @Enumerated(EnumType.STRING)
    private AnnotationStatus status;

    @Column(name = "human_correction", columnDefinition = "TEXT")
    private String humanCorrection;

    @Column(name = "reviewed_by")
    private String reviewedBy;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "review_time")
    private Instant reviewTime;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
        if (this.status == null) {
            this.status = AnnotationStatus.PENDING;
        }
    }

    public enum AnnotationStatus {
        PENDING,
        APPROVED,
        REJECTED,
        REWRITTEN
    }
}
