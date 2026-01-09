package com.neurogate.vault.neuroguard;

/**
 * Interface for Active Defense capabilities (PII detection, Prompt Injection).
 */
public interface ActiveDefenseService {
    String validatePrompt(String prompt);

    com.neurogate.vault.neuroguard.model.ThreatDetectionResult analyzePrompt(String prompt);

    com.neurogate.vault.neuroguard.model.ThreatDetectionResult analyzeOutput(String output);

    String sanitizeOutput(String output);

    SecurityScanResult fullScan(String prompt, String output);

    java.util.Map<String, Object> getStatistics();

    @lombok.Data
    @lombok.Builder
    class SecurityScanResult {
        private com.neurogate.vault.neuroguard.model.ThreatDetectionResult promptAnalysis;
        private com.neurogate.vault.neuroguard.model.ThreatDetectionResult outputAnalysis;
        private String sanitizedOutput;
        private boolean overallBlocked;
    }
}
