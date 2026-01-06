package com.neurogate.vault.detector;

import com.neurogate.vault.model.PiiEntity;
import com.neurogate.vault.model.PiiType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ContextAwarePiiDetector - Detects PII based on surrounding context
 * reducing false positives (e.g., "Apple" the fruit vs "Apple" the Org).
 */
@Slf4j
@Component("contextAwarePiiDetector")
public class ContextAwarePiiDetector implements PiiDetector {

    // Regex for context-aware patterns
    // Capture group 2 is the sensitive data
    private static final List<ContextPattern> PATTERNS = List.of(
            // SSN: Look for "SSN", "Social Security", etc. before the number
            new ContextPattern(
                    Pattern.compile("(?i)(ssn|social security|soc sec)[^a-zA-Z0-9]*([0-9]{3}-[0-9]{2}-[0-9]{4})"),
                    PiiType.SSN, 2, 0.95),

            // API Keys: Look for "api_key", "bearer", etc.
            new ContextPattern(
                    Pattern.compile("(?i)(api[_-]?key|access[_-]?token|bearer)[^a-zA-Z0-9]*([a-zA-Z0-9_\\-]{20,})"),
                    PiiType.API_KEY, 2, 0.99));

    @Override
    public List<PiiEntity> detect(String text) {
        List<PiiEntity> entities = new ArrayList<>();

        for (ContextPattern contextPattern : PATTERNS) {
            Matcher matcher = contextPattern.pattern.matcher(text);
            while (matcher.find()) {
                String value = matcher.group(contextPattern.groupIndex);
                int start = matcher.start(contextPattern.groupIndex);
                int end = matcher.end(contextPattern.groupIndex);

                entities.add(new PiiEntity(
                        contextPattern.type,
                        value,
                        start,
                        end,
                        contextPattern.confidence));
            }
        }

        return entities;
    }

    @Override
    public String getName() {
        return "Context-Aware Detector";
    }

    private record ContextPattern(Pattern pattern, PiiType type, int groupIndex, double confidence) {
    }
}
