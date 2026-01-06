package com.neurogate.core.cortex;

import lombok.RequiredArgsConstructor;
import com.neurogate.sentinel.model.ChatRequest;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class CortexService {

    private final EvaluationDatasetRepository datasetRepository;
    private final EvaluationCaseRepository caseRepository;
    private final EvaluationRunRepository runRepository;
    private final Judge judge;
    private final com.neurogate.sentinel.SentinelService sentinelService;

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

    private EvaluationResult evaluateCase(EvaluationCase evaluationCase, EvaluationRun run) {
        String agentOutput;
        try {
            ChatRequest request = ChatRequest.builder()
                    .model(run.getAgentVersion() != null ? run.getAgentVersion() : "gpt-4")
                    .messages(
                            List.of(new com.neurogate.sentinel.model.Message("user", evaluationCase.getInput(), null)))
                    .build();

            com.neurogate.sentinel.model.ChatResponse response = sentinelService.processRequest(request);

            if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                agentOutput = response.getChoices().get(0).getMessage().getContent().toString();
            } else {
                agentOutput = "Error: No response from model";
            }
        } catch (Exception e) {
            agentOutput = "Error executing agent: " + e.getMessage();
        }

        // Judge logic
        double score = judge.grade(evaluationCase.getInput(), agentOutput, evaluationCase.getIdealOutput());

        EvaluationResult result = new EvaluationResult();
        result.setCaseId(evaluationCase.getId());
        result.setAgentOutput(agentOutput);
        result.setScore((int) (score * 100));
        result.setJudgeReasoning("Automated Judge Grading.");
        result.setRun(run);

        return result;
    }

    public List<EvaluationRun> getRuns(String datasetId) {
        return runRepository.findByDatasetId(datasetId);
    }
}
