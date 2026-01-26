package com.neurogate.consensus;

import com.neurogate.sentinel.model.ChatRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for Hive Mind Consensus operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/hive")
@RequiredArgsConstructor
@Tag(name = "Consensus", description = "Multi-model consensus (Hive Mind)")
public class ConsensusController {

    private final ConsensusService consensusService;

    @Operation(summary = "Reach consensus", description = "Query multiple models and synthesize a consensus response")
    @ApiResponse(responseCode = "200", description = "Consensus reached")
    @PostMapping("/consensus")
    public ResponseEntity<ConsensusResult> reachConsensus(@RequestBody ChatRequest request) {
        log.info("Received consensus request");
        return ResponseEntity.ok(consensusService.reachConsensus(request));
    }
}
