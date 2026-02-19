package com.neurogate.core.cortex;

import com.neurogate.auth.RequiresRole;
import com.neurogate.auth.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/cortex")
@RequiredArgsConstructor
@Tag(name = "Cortex", description = "Response evaluation engine for AI quality assessment")
@RequiresRole(Role.VIEWER)
public class CortexController {

    private final CortexService cortexService;

    @Operation(summary = "Create evaluation dataset", description = "Create a new dataset for evaluation test cases")
    @ApiResponse(responseCode = "200", description = "Dataset created")
    @PostMapping("/datasets")
    @RequiresRole(Role.DEVELOPER)
    public ResponseEntity<EvaluationDataset> createDataset(@RequestBody Map<String, String> request) {
        return ResponseEntity.ok(cortexService.createDataset(request.get("name")));
    }

    @Operation(summary = "Add evaluation case", description = "Add a test case to an evaluation dataset")
    @ApiResponse(responseCode = "200", description = "Case added")
    @PostMapping("/datasets/{id}/cases")
    @RequiresRole(Role.DEVELOPER)
    public ResponseEntity<EvaluationCase> addCase(@Parameter(description = "Dataset ID") @PathVariable String id, @RequestBody EvaluationCase evaluationCase) {
        return ResponseEntity.ok(cortexService.addCase(id, evaluationCase));
    }

    @Operation(summary = "Run evaluation", description = "Execute evaluation against a dataset")
    @ApiResponse(responseCode = "200", description = "Evaluation completed")
    @PostMapping("/runs")
    @RequiresRole(Role.DEVELOPER)
    public ResponseEntity<EvaluationRun> runEvaluation(@RequestBody Map<String, String> request) {
        String datasetId = request.get("datasetId");
        String agentVersion = request.getOrDefault("agentVersion", "v1.0.0");
        return ResponseEntity.ok(cortexService.runEvaluation(datasetId, agentVersion));
    }

    @Operation(summary = "Get evaluation runs", description = "Retrieve all evaluation runs for a dataset")
    @ApiResponse(responseCode = "200", description = "Runs retrieved")
    @GetMapping("/runs/{datasetId}")
    public ResponseEntity<List<EvaluationRun>> getRuns(@Parameter(description = "Dataset ID") @PathVariable String datasetId) {
        return ResponseEntity.ok(cortexService.getRuns(datasetId));
    }
}
