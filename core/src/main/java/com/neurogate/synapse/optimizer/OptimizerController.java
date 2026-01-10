package com.neurogate.synapse.optimizer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/synapse/optimize")
@RequiredArgsConstructor
public class OptimizerController {

    private final OptimizerService optimizerService;

    @PostMapping
    public ResponseEntity<OptimizerResponse> optimize(@RequestBody OptimizerRequest request) {
        return ResponseEntity.ok(optimizerService.optimize(request));
    }
}
