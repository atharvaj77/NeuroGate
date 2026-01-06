package com.neurogate.flywheel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neurogate.flywheel.model.FeedbackRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class DatasetService {

    private final ObjectMapper objectMapper;
    private final Map<String, FeedbackRequest> feedbackStore = new ConcurrentHashMap<>();

    // Mock storage for interactions (In real app, use DB)
    // private final Map<String, Interaction> interactionStore = ...

    public void recordFeedback(FeedbackRequest feedback) {
        log.info("Recording feedback for trace {}: rating={}", feedback.getTraceId(), feedback.getRating());
        feedbackStore.put(feedback.getTraceId(), feedback);
    }

    @Async
    public void exportGoldenDataset() {
        log.info("Starting Golden Dataset export...");
        try (FileWriter writer = new FileWriter("golden_dataset.jsonl")) {
            for (FeedbackRequest feedback : feedbackStore.values()) {
                if (feedback.getRating() != null && feedback.getRating() >= 4) {
                    Map<String, Object> entry = Map.of(
                            "traceId", feedback.getTraceId(),
                            "rating", feedback.getRating(),
                            "output",
                            feedback.getCorrectedOutput() != null ? feedback.getCorrectedOutput() : "ORIGINAL_GOOD");
                    writer.write(objectMapper.writeValueAsString(entry) + "\n");
                }
            }
            log.info("Exported {} high-quality interactions to golden_dataset.jsonl", feedbackStore.size());
        } catch (IOException e) {
            log.error("Failed to export dataset", e);
        }
    }

    public int getFeedbackCount() {
        return feedbackStore.size();
    }

    public List<FeedbackRequest> getGoldenTraces(int minRating) {
        return feedbackStore.values().stream()
                .filter(f -> f.getRating() != null && f.getRating() >= minRating)
                .toList();
    }
}
