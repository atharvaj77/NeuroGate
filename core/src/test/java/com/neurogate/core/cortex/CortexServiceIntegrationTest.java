package com.neurogate.core.cortex;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
public class CortexServiceIntegrationTest {

    @Autowired
    private CortexService cortexService;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private Judge judge; // Mock the Judge to avoid LLM calls

    @Test
    void testEndToEndEvaluationFlow() {
        // 1. Create Dataset
        EvaluationDataset dataset = cortexService.createDataset("Integration Test Suite");
        assertThat(dataset.getId()).isNotNull();

        // 2. Add Case
        EvaluationCase c = new EvaluationCase();
        c.setInput("Test Input");
        c.setIdealOutput("Test Output");
        cortexService.addCase(dataset.getId(), c);

        // Mock Judge
        when(judge.grade(anyString(), anyString(), anyString())).thenReturn(0.9);

        // 3. Run Evaluation
        EvaluationRun run = cortexService.runEvaluation(dataset.getId(), "v1.test");

        // 4. Verify Results
        assertThat(run.getId()).isNotNull();
        assertThat(run.getOverallScore()).isEqualTo(90.0);
        assertThat(run.getResults()).hasSize(1);

        EvaluationResult result = run.getResults().get(0);
        assertThat(result.getScore()).isEqualTo(90);
        assertThat(result.getJudgeReasoning()).contains("Automated Logic");
    }
}
