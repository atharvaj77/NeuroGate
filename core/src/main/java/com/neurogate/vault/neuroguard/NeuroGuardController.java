package com.neurogate.vault.neuroguard;

import com.neurogate.vault.neuroguard.model.ThreatDetectionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Security dashboard and analysis API.
 */
@Slf4j
@RestController
@RequestMapping("/v1/neuroguard")
@RequiredArgsConstructor
public class NeuroGuardController {

    private final ActiveDefenseService activeDefenseService;

    /**
     * Analyze a prompt for security threats
     */
    @PostMapping("/analyze/prompt")
    public ResponseEntity<ThreatDetectionResult> analyzePrompt(@RequestBody AnalyzeRequest request) {
        ThreatDetectionResult result = activeDefenseService.analyzePrompt(request.getContent());
        return ResponseEntity.ok(result);
    }

    /**
     * Analyze output for toxic content
     */
    @PostMapping("/analyze/output")
    public ResponseEntity<ThreatDetectionResult> analyzeOutput(@RequestBody AnalyzeRequest request) {
        ThreatDetectionResult result = activeDefenseService.analyzeOutput(request.getContent());
        return ResponseEntity.ok(result);
    }

    /**
     * Full security scan (prompt + output)
     */
    @PostMapping("/scan")
    public ResponseEntity<ActiveDefenseService.SecurityScanResult> fullScan(@RequestBody ScanRequest request) {
        ActiveDefenseService.SecurityScanResult result = activeDefenseService.fullScan(
                request.getPrompt(),
                request.getOutput());
        return ResponseEntity.ok(result);
    }

    /**
     * Get security statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(activeDefenseService.getStatistics());
    }

    /**
     * Analyze request DTO
     */
    @lombok.Data
    public static class AnalyzeRequest {
        private String content;
    }

    /**
     * Scan request DTO
     */
    @lombok.Data
    public static class ScanRequest {
        private String prompt;
        private String output;
    }
}
