package com.neurogate.consensus;

import com.neurogate.sentinel.model.ChatRequest;
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
public class ConsensusController {

    private final ConsensusService consensusService;

    /**
     * Reaches consensus on a prompt
     */
    @PostMapping("/consensus")
    public ResponseEntity<ConsensusResult> reachConsensus(@RequestBody ChatRequest request) {
        log.info("Received consensus request");
        return ResponseEntity.ok(consensusService.reachConsensus(request));
    }
}
