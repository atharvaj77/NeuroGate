package com.neurogate.experiment;

import com.neurogate.experiment.model.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for managing A/B test experiments.
 */
@RestController
@RequestMapping("/api/v1/experiments")
@RequiredArgsConstructor
@Tag(name = "Experiments", description = "A/B testing for LLM model selection")
public class ExperimentController {

    private final ExperimentService experimentService;

    @PostMapping
    @Operation(
            summary = "Create experiment",
            description = "Create a new A/B test experiment comparing two models"
    )
    @ApiResponse(responseCode = "201", description = "Experiment created")
    public ResponseEntity<Experiment> createExperiment(
            @Valid @RequestBody CreateExperimentRequest request
    ) {
        Experiment experiment = experimentService.createExperiment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(experiment);
    }

    @GetMapping
    @Operation(
            summary = "List experiments",
            description = "List all experiments with optional status filter"
    )
    public List<Experiment> listExperiments(
            @Parameter(description = "Filter by status")
            @RequestParam(required = false) ExperimentStatus status
    ) {
        List<Experiment> experiments = experimentService.listExperiments();

        if (status != null) {
            experiments = experiments.stream()
                    .filter(e -> e.getStatus() == status)
                    .toList();
        }

        return experiments;
    }

    @GetMapping("/active")
    @Operation(
            summary = "List active experiments",
            description = "List experiments that are currently running"
    )
    public List<Experiment> listActiveExperiments() {
        return experimentService.listActiveExperiments();
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get experiment",
            description = "Get experiment details by ID"
    )
    @ApiResponse(responseCode = "200", description = "Experiment found")
    @ApiResponse(responseCode = "404", description = "Experiment not found")
    public ResponseEntity<Experiment> getExperiment(
            @Parameter(description = "Experiment ID")
            @PathVariable String id
    ) {
        return experimentService.getExperiment(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/start")
    @Operation(
            summary = "Start experiment",
            description = "Start an experiment to begin routing traffic"
    )
    @ApiResponse(responseCode = "200", description = "Experiment started")
    @ApiResponse(responseCode = "404", description = "Experiment not found")
    public ResponseEntity<Experiment> startExperiment(
            @Parameter(description = "Experiment ID")
            @PathVariable String id
    ) {
        try {
            Experiment experiment = experimentService.startExperiment(id);
            return ResponseEntity.ok(experiment);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/pause")
    @Operation(
            summary = "Pause experiment",
            description = "Temporarily pause an experiment"
    )
    public ResponseEntity<Experiment> pauseExperiment(
            @Parameter(description = "Experiment ID")
            @PathVariable String id
    ) {
        try {
            Experiment experiment = experimentService.pauseExperiment(id);
            return ResponseEntity.ok(experiment);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/stop")
    @Operation(
            summary = "Stop experiment",
            description = "Stop an experiment permanently"
    )
    public ResponseEntity<Experiment> stopExperiment(
            @Parameter(description = "Experiment ID")
            @PathVariable String id
    ) {
        try {
            Experiment experiment = experimentService.stopExperiment(id);
            return ResponseEntity.ok(experiment);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/results")
    @Operation(
            summary = "Get experiment results",
            description = "Get detailed results and statistics for an experiment"
    )
    @ApiResponse(responseCode = "200", description = "Results retrieved")
    @ApiResponse(responseCode = "404", description = "Experiment not found")
    public ResponseEntity<ExperimentStats> getExperimentResults(
            @Parameter(description = "Experiment ID")
            @PathVariable String id
    ) {
        try {
            ExperimentStats stats = experimentService.calculateStats(id);
            return ResponseEntity.ok(stats);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/results/raw")
    @Operation(
            summary = "Get raw results",
            description = "Get raw result data for an experiment"
    )
    public ResponseEntity<List<ExperimentResult>> getRawResults(
            @Parameter(description = "Experiment ID")
            @PathVariable String id
    ) {
        if (experimentService.getExperiment(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(experimentService.getResults(id));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete experiment",
            description = "Delete an experiment and all its results"
    )
    @ApiResponse(responseCode = "204", description = "Experiment deleted")
    public ResponseEntity<Void> deleteExperiment(
            @Parameter(description = "Experiment ID")
            @PathVariable String id
    ) {
        experimentService.deleteExperiment(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sample-size")
    @Operation(
            summary = "Calculate sample size",
            description = "Calculate required sample size for desired statistical power"
    )
    public SampleSizeResponse calculateSampleSize(
            @Parameter(description = "Expected effect size (Cohen's d, e.g., 0.2=small, 0.5=medium, 0.8=large)")
            @RequestParam(defaultValue = "0.5") double effectSize,
            @Parameter(description = "Desired statistical power (0-1, default 0.8)")
            @RequestParam(defaultValue = "0.8") double power,
            @Parameter(description = "Significance level (default 0.05)")
            @RequestParam(defaultValue = "0.05") double alpha
    ) {
        StatisticalCalculator calculator = new StatisticalCalculator();
        int sampleSize = calculator.requiredSampleSize(effectSize, power, alpha);

        return new SampleSizeResponse(
                effectSize, power, alpha, sampleSize,
                String.format("Need %d samples per variant (%d total) to detect an effect size of %.2f " +
                                "with %.0f%% power at %.0f%% significance level.",
                        sampleSize, sampleSize * 2, effectSize, power * 100, alpha * 100)
        );
    }

    public record SampleSizeResponse(
            double effectSize,
            double power,
            double alpha,
            int requiredSampleSizePerVariant,
            String description
    ) {}
}