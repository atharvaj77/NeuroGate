package com.neurogate.core.cortex.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdHocEvaluationRequest {
    private String promptTemplate;
    private List<TestCase> testCases;
    private String model;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestCase {
        private String id;
        private String input; // User query or variable map in JSON
        private String expectedOutput;
    }
}
