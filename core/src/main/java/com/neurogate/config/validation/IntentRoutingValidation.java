package com.neurogate.config.validation;

import com.neurogate.router.intelligence.IntentRoutingConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Validates intent routing configuration.
 */
@Component
@RequiredArgsConstructor
public class IntentRoutingValidation implements ConfigValidation {

    private final IntentRoutingConfig config;

    @Override
    public ValidationResult validate() {
        if (!config.isEnabled()) {
            return ValidationResult.ok();
        }

        if (config.getConfidenceThreshold() < 0.0 || config.getConfidenceThreshold() > 1.0) {
            return ValidationResult.error(
                    "Intent routing confidence threshold must be between 0.0 and 1.0, got: "
                            + config.getConfidenceThreshold()
            );
        }

        return ValidationResult.ok();
    }

    @Override
    public String getName() {
        return "intent-routing";
    }
}
