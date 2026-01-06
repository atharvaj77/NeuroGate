package com.neurogate.vault.neuroguard;

import com.neurogate.vault.detector.PiiDetector;
import com.neurogate.vault.model.PiiEntity;
import com.neurogate.vault.model.PiiType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Advanced PII Detector that combines Heuristics with ML-ready slots.
 */
@Component
@Primary // Make this the primary detector if available
@Slf4j
public class MlPiiDetector implements PiiDetector {

    // Simulating ML-based Context Awareness with broader patterns
    private static final Pattern CONTEXT_SSN = Pattern
            .compile("(?i)(ssn|social security|social number).*?(\\d{3}-\\d{2}-\\d{4})");

    // Existing regex patterns (reused as fallback/baseline)
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b\\d{3}[-.]?\\d{3}[-.]?\\d{4}\\b");

    @Override
    public List<PiiEntity> detect(String text) {
        List<PiiEntity> entities = new ArrayList<>();

        // 1. Heuristic / "Context-Aware" Check
        detectWithContext(text, entities);

        // 2. Standard Pattern Matching (Baseline)
        detectStandard(text, entities);

        return entities;
    }

    private void detectWithContext(String text, List<PiiEntity> entities) {
        Matcher ssnMatcher = CONTEXT_SSN.matcher(text);
        while (ssnMatcher.find()) {
            // Group 2 is the actual SSN
            String value = ssnMatcher.group(2);
            entities.add(new PiiEntity(
                    PiiType.SSN,
                    value,
                    ssnMatcher.start(2),
                    ssnMatcher.end(2),
                    0.95 // Higher confidence due to context
            ));
        }
    }

    private void detectStandard(String text, List<PiiEntity> entities) {
        // Email
        Matcher emailMatcher = EMAIL_PATTERN.matcher(text);
        while (emailMatcher.find()) {
            addIfNew(entities,
                    new PiiEntity(PiiType.EMAIL, emailMatcher.group(), emailMatcher.start(), emailMatcher.end(), 0.8));
        }

        // Phone
        Matcher phoneMatcher = PHONE_PATTERN.matcher(text);
        while (phoneMatcher.find()) {
            addIfNew(entities,
                    new PiiEntity(PiiType.PHONE, phoneMatcher.group(), phoneMatcher.start(), phoneMatcher.end(), 0.7));
        }
    }

    @Override
    public String getName() {
        return "ML-Hybrid-Detector";
    }

    private void addIfNew(List<PiiEntity> entities, PiiEntity newEntity) {
        boolean exists = entities.stream()
                .anyMatch(e -> e.getStart() == newEntity.getStart() && e.getEnd() == newEntity.getEnd());
        if (!exists) {
            entities.add(newEntity);
        }
    }
}
