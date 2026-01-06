package com.neurogate.core.cortex;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/cortex")
@RequiredArgsConstructor
public class CortexController {

    private final CortexService cortexService;

    @PostMapping("/datasets")
    public ResponseEntity<EvaluationDataset> createDataset(@RequestBody Map<String, String> request) {
        return ResponseEntity.ok(cortexService.createDataset(request.get("name")));
    }

    @PostMapping("/datasets/{id}/cases")
    public ResponseEntity<EvaluationCase> addCase(@PathVariable String id, @RequestBody EvaluationCase evaluationCase) {
        return ResponseEntity.ok(cortexService.addCase(id, evaluationCase));
    }

    @PostMapping("/runs")
    public ResponseEntity<EvaluationRun> runEvaluation(@RequestBody Map<String, String> request) {
        String datasetId = request.get("datasetId");
        String agentVersion = request.getOrDefault("agentVersion", "v1.0.0");
        return ResponseEntity.ok(cortexService.runEvaluation(datasetId, agentVersion));
    }

    @GetMapping("/runs/{datasetId}")
    public ResponseEntity<List<EvaluationRun>> getRuns(@PathVariable String datasetId) {
        return ResponseEntity.ok(cortexService.getRuns(datasetId));
    }
}
