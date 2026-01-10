package com.neurogate.core.cortex;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cortex_results")
@Data
@NoArgsConstructor
public class EvaluationResult {

    @Id
    @GeneratedValue
    @org.hibernate.annotations.UuidGenerator
    private String id;

    @Column(nullable = false)
    private String caseId;

    @Column(columnDefinition = "TEXT")
    private String agentOutput;

    @Column(columnDefinition = "TEXT")
    private String judgeReasoning;

    private int score; // 0-100

    @Column(length = 20)
    private String status; // PASS, FAIL, WARN

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id", nullable = false)
    private EvaluationRun run;

    public EvaluationResult(String caseId, String agentOutput, String judgeReasoning, int score, String status) {
        this.caseId = caseId;
        this.agentOutput = agentOutput;
        this.judgeReasoning = judgeReasoning;
        this.score = score;
        this.status = status;
    }
}
