package com.neurogate.reinforce.controller;

import com.neurogate.reinforce.model.AnnotationTask;
import com.neurogate.reinforce.service.AnnotationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reinforce")
@RequiredArgsConstructor
@Tag(name = "Reinforce", description = "Human-in-the-loop feedback and annotation")
public class ReinforceController {

    private final AnnotationService annotationService;

    @Operation(summary = "Get annotation queue", description = "Retrieve pending annotation tasks for review")
    @ApiResponse(responseCode = "200", description = "Queue retrieved")
    @GetMapping("/queue")
    public ResponseEntity<List<AnnotationTask>> getQueue() {
        return ResponseEntity.ok(annotationService.getPendingTasks());
    }

    @Operation(summary = "Review annotation task", description = "Submit a review for an annotation task")
    @ApiResponse(responseCode = "200", description = "Review submitted")
    @PostMapping("/{id}/review")
    public ResponseEntity<AnnotationTask> reviewTask(
            @Parameter(description = "Task ID") @PathVariable Long id,
            @RequestBody ReviewRequest request) {

        AnnotationTask task = annotationService.reviewTask(
                id,
                request.getStatus(),
                request.getCorrection(),
                request.getReviewer());
        return ResponseEntity.ok(task);
    }

    @Data
    public static class ReviewRequest {
        private AnnotationTask.AnnotationStatus status;
        private String correction;
        private String reviewer;
    }
}
