package com.neurogate.forge.controller;

import com.neurogate.forge.model.DistillationJob;
import com.neurogate.forge.repository.DistillationJobRepository;
import com.neurogate.forge.service.DistillationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/forge")
@RequiredArgsConstructor
@Tag(name = "Forge", description = "Model fine-tuning and distillation")
public class ForgeController {

    private final DistillationService distillationService;
    private final DistillationJobRepository jobRepository;

    @Operation(summary = "Trigger distillation job", description = "Manually trigger a model distillation job")
    @ApiResponse(responseCode = "200", description = "Job triggered")
    @ApiResponse(responseCode = "204", description = "No data available for distillation")
    @PostMapping("/jobs/trigger")
    public ResponseEntity<DistillationJob> triggerDistillation() {
        DistillationJob job = distillationService.triggerManualDistillation();
        if (job == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(job);
    }

    @Operation(summary = "List distillation jobs", description = "Get all distillation jobs")
    @ApiResponse(responseCode = "200", description = "Jobs retrieved")
    @GetMapping("/jobs")
    public List<DistillationJob> getAllJobs() {
        return jobRepository.findAll();
    }

    @Operation(summary = "Get distillation job", description = "Get a specific distillation job by ID")
    @ApiResponse(responseCode = "200", description = "Job found")
    @ApiResponse(responseCode = "404", description = "Job not found")
    @GetMapping("/jobs/{id}")
    public ResponseEntity<DistillationJob> getJob(@Parameter(description = "Job ID") @PathVariable UUID id) {
        return jobRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
