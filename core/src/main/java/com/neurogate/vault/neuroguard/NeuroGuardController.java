package com.neurogate.vault.neuroguard;

import com.neurogate.vault.neuroguard.model.ThreatDetectionResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "NeuroGuard", description = "PII detection and security threat analysis")
public class NeuroGuardController {

    private final ActiveDefenseService activeDefenseService;

    @Operation(summary = "Analyze prompt for threats", description = "Detect security threats including prompt injection and jailbreak attempts")
    @ApiResponse(responseCode = "200", description = "Analysis completed")
    @PostMapping("/analyze/prompt")
    public ResponseEntity<ThreatDetectionResult> analyzePrompt(@RequestBody AnalyzeRequest request) {
        ThreatDetectionResult result = activeDefenseService.analyzePrompt(request.getContent());
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Analyze output for toxicity", description = "Detect toxic content, hate speech, and harmful content in LLM outputs")
    @ApiResponse(responseCode = "200", description = "Analysis completed")
    @PostMapping("/analyze/output")
    public ResponseEntity<ThreatDetectionResult> analyzeOutput(@RequestBody AnalyzeRequest request) {
        ThreatDetectionResult result = activeDefenseService.analyzeOutput(request.getContent());
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Full security scan", description = "Comprehensive security scan of both prompt and output")
    @ApiResponse(responseCode = "200", description = "Scan completed")
    @PostMapping("/scan")
    public ResponseEntity<ActiveDefenseService.SecurityScanResult> fullScan(@RequestBody ScanRequest request) {
        ActiveDefenseService.SecurityScanResult result = activeDefenseService.fullScan(
                request.getPrompt(),
                request.getOutput());
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Get security statistics", description = "Retrieve aggregate security statistics and threat counts")
    @ApiResponse(responseCode = "200", description = "Statistics retrieved")
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
