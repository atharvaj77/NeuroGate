package com.neurogate.core.cortex;

import com.neurogate.tenant.TenantScopedEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cortex_datasets")
@Data
@NoArgsConstructor
public class EvaluationDataset extends TenantScopedEntity {

    @Id
    @GeneratedValue
    @org.hibernate.annotations.UuidGenerator
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "dataset", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EvaluationCase> cases = new ArrayList<>();

    public void addCase(EvaluationCase evaluationCase) {
        cases.add(evaluationCase);
        evaluationCase.setDataset(this);
    }
}
