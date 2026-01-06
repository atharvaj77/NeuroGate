package com.neurogate.forge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neurogate.flywheel.DatasetService;
import com.neurogate.flywheel.model.FeedbackRequest;
import com.neurogate.forge.config.ForgeConfig;
import com.neurogate.forge.model.DistillationJob;
import com.neurogate.forge.provider.TrainingProvider;
import com.neurogate.forge.repository.DistillationJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class DistillationService {

    private final DatasetService datasetService;
    private final ForgeConfig forgeConfig;
    private final DistillationJobRepository jobRepository;
    private final TrainingProvider trainingProvider;
    private final com.neurogate.agentops.TraceService traceService;
    private final ObjectMapper objectMapper;

    @Scheduled(cron = "${neurogate.forge.schedule-cron:0 0 2 * * ?}")
    public void runNightlyCycle() {
        if (!forgeConfig.isEnabled()) {
            log.info("Forge distillation is disabled. Skipping nightly cycle.");
            return;
        }

        log.info("Starting nightly distillation cycle...");

        log.info("Starting nightly distillation cycle...");

        List<FeedbackRequest> goldenTraces = datasetService.getGoldenTraces(4);
        int datasetSize = goldenTraces.size();
        log.info("Found {} golden traces. Threshold is {}", datasetSize, forgeConfig.getTriggerThreshold());

        if (datasetSize < forgeConfig.getTriggerThreshold()) {
            log.info("Not enough golden traces to trigger training. Found: {}, Required: {}",
                    datasetSize, forgeConfig.getTriggerThreshold());
            return;
        }

        DistillationJob job = DistillationJob.builder()
                .jobId("pending-upload")
                .status(DistillationJob.JobStatus.COLLECTING)
                .datasetSize(datasetSize)
                .baseModel(forgeConfig.getStudentBaseModel())
                .build();
        jobRepository.save(job);

        try {
            File trainingFile = createJsonLFile(goldenTraces);
            job.setStatus(DistillationJob.JobStatus.UPLOADING);
            jobRepository.save(job);

            jobRepository.save(job);

            String fileId = trainingProvider.uploadFile(trainingFile);

            job.setStatus(DistillationJob.JobStatus.TRAINING);
            Map<String, Object> trainResponse = trainingProvider.startTrainingJob(fileId,
                    forgeConfig.getStudentBaseModel());

            String providerJobId = (String) trainResponse.get("id");
            job.setJobId(providerJobId); // Update with real ID
            jobRepository.save(job);

            log.info("Distillation job triggered successfully. Job ID: {}", providerJobId);

            // Clean up temp file
            if (trainingFile.exists()) {
                trainingFile.delete();
            }

        } catch (Exception e) {
            log.error("Distillation cycle failed", e);
            job.setStatus(DistillationJob.JobStatus.FAILED);
            job.setEvalMetrics(Map.of("error", 1.0)); // Hack to store error
            jobRepository.save(job);
        }
    }

    private File createJsonLFile(List<FeedbackRequest> traces) throws IOException {
        File tempFile = File.createTempFile("training_data_", ".jsonl");
        try (FileWriter writer = new FileWriter(tempFile)) {
            for (FeedbackRequest trace : traces) {
                // OpenAI Chat Format: messages=[{role, content}, ...]
                // We map FeedbackRequest to this format
                // Assumption: FeedbackRequest contains enough info to reconstruct the chat
                // For now, we'll construct a simple User -> Assistant pair based on
                // input/output

                // Fetch original prompt from trace
                String userContent = "Trace: " + trace.getTraceId();
                try {
                    java.util.Optional<com.neurogate.agentops.model.Trace> originalTrace = traceService
                            .getTrace(trace.getTraceId());
                    if (originalTrace.isPresent() && !originalTrace.get().getSpans().isEmpty()) {
                        // Assumption: First span content or specific field contains the prompt
                        // Ideally: Trace object should have a top-level 'prompt' or 'input' field
                        // For now, we fallback to a generic message if strict prompt missing
                        userContent = originalTrace.get().getSpans().get(0).getInput();
                        if (userContent == null)
                            userContent = "Trace Reference: " + trace.getTraceId();
                    }
                } catch (Exception e) {
                    log.warn("Could not fetch trace details for {}", trace.getTraceId());
                }

                Map<String, Object> userMsg = Map.of("role", "user", "content", userContent);

                String output = trace.getCorrectedOutput() != null ? trace.getCorrectedOutput() : "Good response";

                Map<String, Object> assistantMsg = Map.of("role", "assistant", "content", output);

                Map<String, Object> trainingExample = Map.of("messages", List.of(userMsg, assistantMsg));

                writer.write(objectMapper.writeValueAsString(trainingExample) + "\n");
            }
        }
        return tempFile;
    }

    public DistillationJob triggerManualDistillation() {
        runNightlyCycle();
        // Since runNightlyCycle is void and async-ish in logic (though synchronous
        // here),
        // we return the latest job or null.
        // Better design: separate logic from scheduling.
        // For MVP, checking DB.
        return jobRepository.findAll().stream()
                .filter(j -> j.getCreatedAt().isAfter(Instant.now().minusSeconds(10)))
                .findFirst()
                .orElse(null);
    }
}
