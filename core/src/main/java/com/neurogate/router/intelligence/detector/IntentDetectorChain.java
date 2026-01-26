package com.neurogate.router.intelligence.detector;

import com.neurogate.router.intelligence.model.Intent;
import com.neurogate.router.intelligence.model.IntentClassification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Chains multiple intent detectors together.
 * Detectors are tried in priority order until one matches.
 *
 * <p>This class implements the Chain of Responsibility pattern,
 * allowing new intent detectors to be added without modifying existing code.</p>
 */
@Slf4j
@Component
public class IntentDetectorChain {

    private final List<IntentDetector> detectors;

    public IntentDetectorChain(List<IntentDetector> detectors) {
        this.detectors = detectors.stream()
                .sorted(Comparator.comparingInt(IntentDetector::getPriority))
                .toList();

        log.info("Initialized intent detector chain with {} detectors: {}",
                this.detectors.size(),
                this.detectors.stream().map(IntentDetector::getName).toList());
    }

    /**
     * Classify intent using all registered detectors.
     *
     * @param prompt the prompt to classify
     * @return classification result
     */
    public IntentClassification classify(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return defaultClassification();
        }

        Map<Intent, Double> allScores = new EnumMap<>(Intent.class);
        List<String> matchedFeatures = new ArrayList<>();
        IntentDetector.DetectionResult bestMatch = null;

        for (IntentDetector detector : detectors) {
            if (!detector.isEnabled()) {
                continue;
            }

            try {
                Optional<IntentDetector.DetectionResult> result = detector.detect(prompt);

                if (result.isPresent()) {
                    IntentDetector.DetectionResult detection = result.get();
                    allScores.put(detection.intent(), detection.confidence());
                    matchedFeatures.add(detector.getName() + ":" + detection.matchedPattern());

                    if (bestMatch == null || detection.confidence() > bestMatch.confidence()) {
                        bestMatch = detection;
                    }

                    log.debug("Detector '{}' matched: {} (confidence: {:.2f})",
                            detector.getName(), detection.intent(), detection.confidence());
                }
            } catch (Exception e) {
                log.warn("Detector '{}' failed: {}", detector.getName(), e.getMessage());
            }
        }

        if (bestMatch == null) {
            return defaultClassification();
        }

        return IntentClassification.builder()
                .intent(bestMatch.intent())
                .confidence(bestMatch.confidence())
                .allScores(allScores)
                .matchedFeatures(matchedFeatures)
                .build();
    }

    private IntentClassification defaultClassification() {
        return IntentClassification.builder()
                .intent(Intent.CONVERSATION)
                .confidence(0.0)
                .allScores(Map.of())
                .matchedFeatures(List.of())
                .build();
    }

    /**
     * Get all registered detectors.
     */
    public List<IntentDetector> getDetectors() {
        return detectors;
    }
}
