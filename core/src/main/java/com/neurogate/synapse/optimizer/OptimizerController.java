package com.neurogate.synapse.optimizer;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/synapse/optimize")
@RequiredArgsConstructor
@Tag(name = "Synapse", description = "Prompt optimization, versioning, and deployment")
public class OptimizerController {

    private final OptimizerService optimizerService;

    @Operation(summary = "Optimize prompt", description = "Analyze and optimize a prompt for better performance")
    @ApiResponse(responseCode = "200", description = "Optimization completed")
    @PostMapping
    public ResponseEntity<OptimizerResponse> optimize(@RequestBody OptimizerRequest request) {
        return ResponseEntity.ok(optimizerService.optimize(request));
    }
}
