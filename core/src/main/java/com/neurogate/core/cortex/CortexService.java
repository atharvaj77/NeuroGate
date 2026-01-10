package com.neurogate.core.cortex;

import com.neurogate.core.cortex.dto.AdHocEvaluationRequest;
import com.neurogate.core.cortex.dto.AdHocEvaluationResponse;
import com.neurogate.sentinel.model.ChatRequest;
import com.neurogate.sentinel.model.ChatResponse;
import com.neurogate.sentinel.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CortexService {

    private final EvaluationDatasetRepository datasetRepository;
    private final EvaluationCaseRepository caseRepository;
    private final EvaluationRunRepository runRepository;
    private final com.neurogate.sentinel.SentinelService sentinelService;
    private final Map<String, Judge> judgeMap;

    @Transactional
    public EvaluationDataset createDataset(String name) {
        EvaluationDataset dataset = new EvaluationDataset();
        dataset.setName(name);
        return datasetRepository.save(dataset);
    }

    @Transactional
    public EvaluationCase addCase(String datasetId, EvaluationCase evaluationCase) {
        EvaluationDataset dataset = datasetRepository.findById(datasetId)
                .orElseThrow(() -> new IllegalArgumentException("Dataset not found: " + datasetId));
        dataset.addCase(evaluationCase);
        return caseRepository.save(evaluationCase);
    }

    @Transactional
    public EvaluationRun runEvaluation(String datasetId, String agentVersion) {
        EvaluationDataset dataset = datasetRepository.findById(datasetId)
                .orElseThrow(() -> new IllegalArgumentException("Dataset not found: " + datasetId));

        EvaluationRun run = new EvaluationRun();
        run.setDatasetId(datasetId);
        run.setAgentVersion(agentVersion);
        run.setCreatedAt(LocalDateTime.now());

        // Save first to generate ID
        final EvaluationRun savedRun = runRepository.save(run);

        // Run evaluation logic (Parallel)
        List<CompletableFuture<EvaluationResult>> futures = dataset.getCases().stream()
                .map(evaluationCase -> CompletableFuture.supplyAsync(() -> evaluateCase(evaluationCase, savedRun)))
                .toList();

        List<EvaluationResult> results = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        savedRun.getResults().addAll(results);

        // Calculate overall score
        double averageScore = results.stream()
                .mapToInt(EvaluationResult::getScore)
                .average()
                .orElse(0.0);

        savedRun.setOverallScore(averageScore);

        return runRepository.save(savedRun);
    }

    /**
     * Runs an ad-hoc evaluation without persisting to the database.
     * Useful for testing prompts in the Synapse UI.
     */
    public AdHocEvaluationResponse evaluateAdHoc(AdHocEvaluationRequest request) {
        log.info("Running ad-hoc evaluation for model: {}", request.getModel());

        List<CompletableFuture<AdHocEvaluationResponse.CaseResult>> futures = request.getTestCases().stream()
                .map(testCase -> CompletableFuture.supplyAsync(
                        () -> evaluateAdHocCase(testCase, request.getPromptTemplate(), request.getModel())))
                .toList();

        List<AdHocEvaluationResponse.CaseResult> results = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        double averageScore = results.stream()
                .mapToInt(AdHocEvaluationResponse.CaseResult::getScore)
                .average()
                .orElse(0.0);

        return AdHocEvaluationResponse.builder()
                .results(results)
                .overallScore(averageScore)
                .build();
    }

    private AdHocEvaluationResponse.CaseResult evaluateAdHocCase(AdHocEvaluationRequest.TestCase testCase,
            String promptTemplate, String model) {
        String agentOutput;
        String finalPrompt = promptTemplate;
        // Simple variable substitution (naive implementation, should use real
        // templating engine)
        // Assuming testCase.input might be "user_query=hello" or just "hello"
        // For now, let's assume specific substitution if variables exist, else just
        // append?
        // Actually, the simplest way for "Synapse" integration is:
        // The Synapse UI sends the *compiled* prompt if it wants, OR we do substitution
        // here.
        // Let's assume the Synapse UI sends the raw template and we substitute {{
        // user_query }} with ALL of input?
        // Or better: Synapse UI handles compilation for "Preview", but for "Tests", we
        // need to inject variables.
        // Let's assume input is the value for {{ user_query }} for simplicity in mvp.

        finalPrompt = finalPrompt.replace("{{ user_query }}", testCase.getInput());
        // Also handle {{ date }} etc if needed, but let's stick to user_query for now.

        try {
            ChatRequest request = ChatRequest.builder()
                    .model(model != null ? model : "gpt-4")
                    .messages(List.of(Message.user(finalPrompt)))
                    .temperature(0.0) // Deterministic for testing
                    .build();

            ChatResponse response = sentinelService.processRequest(request);

            if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                agentOutput = response.getChoices().get(0).getMessage().getStrContent();
            } else {
                agentOutput = "Error: No response from model";
            }
        } catch (Exception e) {
            log.error("Error executing ad-hoc case", e);
            agentOutput = "Error: " + e.getMessage();
        }

        // Judge logic
        Judge primaryJudge = judgeMap.getOrDefault("faithfulness", judgeMap.values().iterator().next());
        JudgeResult judgeResult = primaryJudge.grade(testCase.getInput(), agentOutput, testCase.getExpectedOutput());

        return AdHocEvaluationResponse.CaseResult.builder()
                .caseId(testCase.getId())
                .input(testCase.getInput())
                .actualOutput(agentOutput)
                .expectedOutput(testCase.getExpectedOutput())
                .score((int) (judgeResult.getScore() * 100))
                .passed(judgeResult.isPass())
                .reason(judgeResult.getReasoning())
                .build();
    }

    private EvaluationResult evaluateCase(EvaluationCase evaluationCase, EvaluationRun run) {
        String agentOutput;
        try {
            ChatRequest request = ChatRequest.builder()
                    .model(run.getAgentVersion() != null ? run.getAgentVersion() : "gpt-4")
                    .messages(
                            List.of(Message.user(evaluationCase.getInput())))
                    .build();

            ChatResponse response = sentinelService.processRequest(request);

            if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                agentOutput = response.getChoices().get(0).getMessage().getStrContent();
            } else {
                agentOutput = "Error: No response from model";
            }
        } catch (Exception e) {
            agentOutput = "Error executing agent: " + e.getMessage();
        }

        // Judge logic - Default to faithfulness for now or configurable
        Judge primaryJudge = judgeMap.getOrDefault("faithfulness", judgeMap.values().iterator().next());
        JudgeResult judgeResult = primaryJudge.grade(evaluationCase.getInput(), agentOutput,
                evaluationCase.getIdealOutput());

        EvaluationResult result = new EvaluationResult();
        result.setCaseId(evaluationCase.getId());
        result.setAgentOutput(agentOutput);
        result.setScore((int) (judgeResult.getScore() * 100));
        result.setJudgeReasoning(judgeResult.getReasoning());
        result.setStatus(judgeResult.getStatus());
        result.setRun(run);

        return result;
    }

    public List<EvaluationRun> getRuns(String datasetId) {
        return runRepository.findByDatasetId(datasetId);
    }
}
