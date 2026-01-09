package com.neurogate.vault.neuroguard;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnMissingBean(type = "com.neurogate.enterprise.vault.neuroguard.NeuroGuardService")
public class NoOpActiveDefenseService implements ActiveDefenseService {

    @Override
    public String validatePrompt(String prompt) {
        // No-op for Community Edition
        log.trace("Active Defense check skipped (Community Edition)");
        return prompt;
    }

    @Override
    public com.neurogate.vault.neuroguard.model.ThreatDetectionResult analyzePrompt(String prompt) {
        return com.neurogate.vault.neuroguard.model.ThreatDetectionResult.safe();
    }

    @Override
    public com.neurogate.vault.neuroguard.model.ThreatDetectionResult analyzeOutput(String output) {
        return com.neurogate.vault.neuroguard.model.ThreatDetectionResult.safe();
    }

    @Override
    public String sanitizeOutput(String output) {
        return output;
    }

    @Override
    public SecurityScanResult fullScan(String prompt, String output) {
        return SecurityScanResult.builder()
                .promptAnalysis(com.neurogate.vault.neuroguard.model.ThreatDetectionResult.safe())
                .outputAnalysis(com.neurogate.vault.neuroguard.model.ThreatDetectionResult.safe())
                .sanitizedOutput(output)
                .overallBlocked(false)
                .build();
    }

    @Override
    public java.util.Map<String, Object> getStatistics() {
        return java.util.Collections.emptyMap();
    }
}
