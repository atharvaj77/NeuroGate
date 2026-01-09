package com.neurogate.vault.neuroguard;

import com.neurogate.vault.neuroguard.model.ThreatDetectionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * NeuroGuardService - Main security orchestration service.
 */
@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class NeuroGuardService implements ActiveDefenseService {

    private final PromptInjectionDetector injectionDetector;
    private final JailbreakDetector jailbreakDetector;
    private final com.neurogate.vault.detector.ContextAwarePiiDetector piiDetector;
    private final ToxicOutputFilter toxicOutputFilter;

    // Security metrics
    private final AtomicLong totalScans = new AtomicLong(0);
    private final AtomicLong threatsDetected = new AtomicLong(0);
    private final AtomicLong requestsBlocked = new AtomicLong(0);
    private final Map<ThreatDetectionResult.ThreatType, AtomicLong> threatsByType = new ConcurrentHashMap<>();

    /**
     * Validate a prompt for security threats.
     */
    @Override
    public String validatePrompt(String prompt) {
        ThreatDetectionResult result = analyzePrompt(prompt);

        if (result.isBlocked()) {
            log.error("Request blocked due to security threat: {}", result.getThreatType());
            throw new SecurityThreatException(result);
        }

        // Return sanitized content if available, otherwise original
        return result.getSanitizedContent() != null ? result.getSanitizedContent() : prompt;
    }

    /**
     * Comprehensive prompt analysis.
     */
    public ThreatDetectionResult analyzePrompt(String prompt) {
        totalScans.incrementAndGet();

        ThreatDetectionResult injectionResult = injectionDetector.analyze(prompt);
        ThreatDetectionResult jailbreakResult = jailbreakDetector.analyze(prompt);

        // PII Detection
        ThreatDetectionResult piiResult = scanForPii(prompt);

        ThreatDetectionResult result = getHighestConfidenceResult(injectionResult, jailbreakResult, piiResult);

        if (result.isThreatDetected()) {
            threatsDetected.incrementAndGet();
            threatsByType.computeIfAbsent(result.getThreatType(), k -> new AtomicLong(0)).incrementAndGet();

            if (result.isBlocked()) {
                requestsBlocked.incrementAndGet();
            }
        }

        return result;
    }

    private ThreatDetectionResult scanForPii(String prompt) {
        java.util.List<com.neurogate.vault.model.PiiEntity> entities = piiDetector.detect(prompt);

        if (entities.isEmpty()) {
            return ThreatDetectionResult.safe();
        }

        // Use highest confidence entity for the result
        com.neurogate.vault.model.PiiEntity topEntity = entities.stream()
                .max(java.util.Comparator.comparingDouble(com.neurogate.vault.model.PiiEntity::getConfidence))
                .orElseThrow();

        // Perform Masking (Token Replacement)
        String maskedContent = maskPii(prompt, entities);

        return ThreatDetectionResult.builder()
                .threatDetected(true)
                .threatType(ThreatDetectionResult.ThreatType.PII_LEAK)
                .confidenceScore(topEntity.getConfidence())
                .message("PII Detected & Masked: " + entities.size() + " entities found (Top: " + topEntity.getType()
                        + ")")
                .matchedPatterns(entities.stream().map(com.neurogate.vault.model.PiiEntity::getValue).toList())
                .sanitizedContent(maskedContent)
                .blocked(false) // Do not block, we masked it!
                .build();
    }

    /**
     * Replaces detected PII entities with tokens <TYPE>.
     */
    public String maskPii(String prompt, java.util.List<com.neurogate.vault.model.PiiEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return prompt;
        }

        // Sort entities by start index descending to avoid shifting indices
        entities.sort((a, b) -> Integer.compare(b.getStart(), a.getStart()));

        StringBuilder sb = new StringBuilder(prompt);
        for (com.neurogate.vault.model.PiiEntity entity : entities) {
            String token = "<" + entity.getType().name() + ">";
            sb.replace(entity.getStart(), entity.getEnd(), token);
        }
        return sb.toString();
    }

    /**
     * Analyze LLM output for toxic content.
     */
    public ThreatDetectionResult analyzeOutput(String output) {
        return toxicOutputFilter.analyze(output);
    }

    /**
     * Sanitize output by removing sensitive information
     */
    public String sanitizeOutput(String output) {
        return toxicOutputFilter.sanitize(output);
    }

    /**
     * Get security statistics.
     */
    public Map<String, Object> getStatistics() {
        Map<String, Long> threatCounts = new ConcurrentHashMap<>();
        threatsByType.forEach((type, count) -> threatCounts.put(type.name(), count.get()));

        return Map.of(
                "total_scans", totalScans.get(),
                "threats_detected", threatsDetected.get(),
                "requests_blocked", requestsBlocked.get(),
                "threats_by_type", threatCounts,
                "block_rate", totalScans.get() > 0
                        ? (double) requestsBlocked.get() / totalScans.get()
                        : 0.0);
    }

    /**
     * Full security scan (prompt + output)
     */
    public SecurityScanResult fullScan(String prompt, String output) {
        ThreatDetectionResult promptResult = analyzePrompt(prompt);
        ThreatDetectionResult outputResult = analyzeOutput(output);
        String sanitizedOutput = sanitizeOutput(output);

        return SecurityScanResult.builder()
                .promptAnalysis(promptResult)
                .outputAnalysis(outputResult)
                .sanitizedOutput(sanitizedOutput)
                .overallBlocked(promptResult.isBlocked() || outputResult.isBlocked())
                .build();
    }

    private ThreatDetectionResult getHighestConfidenceResult(ThreatDetectionResult... results) {
        ThreatDetectionResult highest = ThreatDetectionResult.safe();

        for (ThreatDetectionResult result : results) {
            if (result.getConfidenceScore() > highest.getConfidenceScore()) {
                highest = result;
            }
        }

        return highest;
    }

    /**
     * Exception thrown when a security threat causes a block
     */
    public static class SecurityThreatException extends RuntimeException {
        private final ThreatDetectionResult result;

        public SecurityThreatException(ThreatDetectionResult result) {
            super("Security threat detected: " + result.getThreatType() + " - " + result.getMessage());
            this.result = result;
        }

        public ThreatDetectionResult getResult() {
            return result;
        }
    }
}
