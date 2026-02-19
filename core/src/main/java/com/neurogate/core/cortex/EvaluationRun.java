package com.neurogate.core.cortex;

import com.neurogate.tenant.TenantScopedEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cortex_runs")
@Data
@NoArgsConstructor
public class EvaluationRun extends TenantScopedEntity {

    @Id
    @GeneratedValue
    @org.hibernate.annotations.UuidGenerator
    private String id;

    @Column(nullable = false)
    private String datasetId;

    @Column(nullable = false)
    private String agentVersion;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private Double overallScore;

    @OneToMany(mappedBy = "run", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EvaluationResult> results = new ArrayList<>();

    public void addResult(EvaluationResult result) {
        results.add(result);
        result.setRun(this);
    }
}
