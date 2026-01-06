package com.neurogate.forge.controller;

import com.neurogate.forge.model.DistillationJob;
import com.neurogate.forge.repository.DistillationJobRepository;
import com.neurogate.forge.service.DistillationService;
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
public class ForgeController {

    private final DistillationService distillationService;
    private final DistillationJobRepository jobRepository;

    @PostMapping("/jobs/trigger")
    public ResponseEntity<DistillationJob> triggerDistillation() {
        DistillationJob job = distillationService.triggerManualDistillation();
        if (job == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(job);
    }

    @GetMapping("/jobs")
    public List<DistillationJob> getAllJobs() {
        return jobRepository.findAll();
    }

    @GetMapping("/jobs/{id}")
    public ResponseEntity<DistillationJob> getJob(@PathVariable UUID id) {
        return jobRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
