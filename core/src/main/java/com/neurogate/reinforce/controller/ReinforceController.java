package com.neurogate.reinforce.controller;

import com.neurogate.reinforce.model.AnnotationTask;
import com.neurogate.reinforce.service.AnnotationService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reinforce")
@RequiredArgsConstructor
public class ReinforceController {

    private final AnnotationService annotationService;

    @GetMapping("/queue")
    public ResponseEntity<List<AnnotationTask>> getQueue() {
        return ResponseEntity.ok(annotationService.getPendingTasks());
    }

    @PostMapping("/{id}/review")
    public ResponseEntity<AnnotationTask> reviewTask(
            @PathVariable Long id,
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
