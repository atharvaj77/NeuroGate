package com.neurogate.router.intelligence.detector;

import com.neurogate.router.intelligence.model.Intent;

import java.util.Optional;

/**
 * Plugin interface for intent detection.
 * Implementations detect specific intent types from prompts.
 *
 * <p>New intent detectors can be added by implementing this interface
 * and annotating with {@code @Component}. The IntentDetectorChain will
 * automatically discover and use all implementations.</p>
 *
 * <p>Example implementation:</p>
 * <pre>{@code
 * @Component
 * public class CustomIntentDetector implements IntentDetector {
 *     @Override
 *     public Optional<DetectionResult> detect(String prompt) {
 *         if (prompt.contains("my-pattern")) {
 *             return Optional.of(new DetectionResult(Intent.CUSTOM, 0.9, "matched pattern"));
 *         }
 *         return Optional.empty();
 *     }
 * }
 * }</pre>
 */
public interface IntentDetector {

    /**
     * Attempt to detect intent from the given prompt.
     *
     * @param prompt the user's prompt text
     * @return detection result if this detector matched, empty otherwise
     */
    Optional<DetectionResult> detect(String prompt);

    /**
     * Get the priority of this detector (lower = runs first).
     * Detectors with lower priority values are checked first.
     */
    default int getPriority() {
        return 100;
    }

    /**
     * Get the name of this detector for logging/metrics.
     */
    String getName();

    /**
     * Check if this detector is enabled.
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * Result of intent detection.
     */
    record DetectionResult(
            Intent intent,
            double confidence,
            String matchedPattern
    ) {}
}
