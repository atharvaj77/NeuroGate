package com.neurogate.flywheel;

import com.neurogate.flywheel.model.FeedbackRequest;
import com.neurogate.flywheel.model.GoldenInteraction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * FlywheelController - REST API for feedback and dataset management
 */
@Slf4j
@RestController
@RequestMapping("/v1/flywheel")
@RequiredArgsConstructor
public class FlywheelController {

    private final FeedbackService feedbackService;
    private final DatasetExporter datasetExporter;
    private final DatasetService datasetService;

    /**
     * Submit feedback for an interaction
     */
    @PostMapping("/feedback")
    public ResponseEntity<GoldenInteraction> submitFeedback(@RequestBody FeedbackRequest request) {

        String feedbackText = request.getFeedbackText();
        if (feedbackText == null && request.getComment() != null) {
            feedbackText = request.getComment();
        }

        GoldenInteraction interaction = GoldenInteraction.builder()
                .id(UUID.randomUUID().toString())
                .traceId(request.getTraceId())
                .sessionId(request.getSessionId())
                .timestamp(Instant.now())
                .messages(request.getMessages())
                .response(request.getResponse())
                .rating(request.getRating())
                .feedbackText(feedbackText)
                .feedbackType(request.getFeedbackType())
                .model(request.getModel())
                .provider(request.getProvider())
                .tokenCount(request.getTokenCount())
                .userId(request.getUserId())
                .tags(request.getTags())
                .correction(request.getCorrectedOutput())
                .build();

        feedbackService.saveInteraction(interaction);

        feedbackService.saveInteraction(interaction);

        datasetService.recordFeedback(request);

        return ResponseEntity.ok(interaction);
    }

    /**
     * Export golden dataset for feedback controller alias
     */
    @PostMapping("/export-trigger")
    public ResponseEntity<String> triggerExport() {
        datasetService.exportGoldenDataset();
        return ResponseEntity.ok("Export started");
    }

    /**
     * Get all golden interactions
     */
    @GetMapping("/golden")
    public ResponseEntity<List<GoldenInteraction>> getGoldenInteractions() {
        return ResponseEntity.ok(feedbackService.getGoldenInteractions());
    }

    /**
     * Export dataset as JSONL
     */
    @GetMapping(value = "/export", produces = "application/jsonl")
    public ResponseEntity<String> exportDataset(
            @RequestParam(required = false) String model) {
        try {
            String jsonl = model != null
                    ? datasetExporter.exportAsJsonlForModel(model)
                    : datasetExporter.exportAsJsonl();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=training_data.jsonl")
                    .contentType(MediaType.parseMediaType("application/jsonl"))
                    .body(jsonl);
        } catch (Exception e) {
            log.error("Failed to export dataset", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get export statistics
     */
    @GetMapping("/export/stats")
    public ResponseEntity<Map<String, Object>> getExportStats() {
        return ResponseEntity.ok(datasetExporter.getExportStats());
    }

    /**
     * Get feedback statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getFeedbackStats() {
        return ResponseEntity.ok(feedbackService.getStatistics());
    }
}
