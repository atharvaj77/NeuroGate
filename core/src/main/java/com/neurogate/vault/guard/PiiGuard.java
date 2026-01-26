package com.neurogate.vault.guard;

import com.neurogate.vault.detector.ContextAwarePiiDetector;
import com.neurogate.vault.model.PiiEntity;
import com.neurogate.vault.neuroguard.model.ThreatDetectionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Security guard for PII detection.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PiiGuard implements SecurityGuard {

    private final ContextAwarePiiDetector piiDetector;

    @Override
    public ThreatDetectionResult check(String content) {
        List<PiiEntity> entities = piiDetector.detect(content);

        if (entities.isEmpty()) {
            return ThreatDetectionResult.safe();
        }

        PiiEntity topEntity = entities.stream()
                .max(Comparator.comparingDouble(PiiEntity::getConfidence))
                .orElseThrow();

        String maskedContent = maskPii(content, entities);

        return ThreatDetectionResult.builder()
                .threatDetected(true)
                .threatType(ThreatDetectionResult.ThreatType.PII_LEAK)
                .confidenceScore(topEntity.getConfidence())
                .message("PII Detected & Masked: " + entities.size() + " entities")
                .matchedPatterns(entities.stream().map(PiiEntity::getValue).toList())
                .sanitizedContent(maskedContent)
                .blocked(false) // Mask, don't block
                .build();
    }

    @Override
    public GuardType getType() {
        return GuardType.PII_DETECTION;
    }

    @Override
    public int getPriority() {
        return 10; // Run early to mask PII before other checks
    }

    private String maskPii(String content, List<PiiEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return content;
        }

        // Sort by start index descending to avoid shifting indices
        entities.sort((a, b) -> Integer.compare(b.getStart(), a.getStart()));

        StringBuilder sb = new StringBuilder(content);
        for (PiiEntity entity : entities) {
            String token = "<" + entity.getType().name() + ">";
            sb.replace(entity.getStart(), entity.getEnd(), token);
        }
        return sb.toString();
    }
}
