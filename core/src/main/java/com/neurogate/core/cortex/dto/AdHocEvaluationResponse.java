package com.neurogate.core.cortex.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class AdHocEvaluationResponse {
    private List<CaseResult> results;
    private double overallScore;

    @Data
    @Builder
    public static class CaseResult {
        private String caseId;
        private String input;
        private String actualOutput;
        private String expectedOutput;
        private boolean passed;
        private int score;
        private String reason;
    }
}
