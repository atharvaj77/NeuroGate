package com.neurogate.config.validation;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates application configuration at startup.
 * Fails fast if critical configuration is invalid.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConfigurationValidator {

    private final List<ConfigValidation> validations;

    @PostConstruct
    public void validate() {
        log.info("Validating application configuration...");

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (ConfigValidation validation : validations) {
            try {
                ValidationResult result = validation.validate();
                if (!result.isValid()) {
                    if (result.isCritical()) {
                        errors.addAll(result.getErrors());
                    } else {
                        warnings.addAll(result.getErrors());
                    }
                }
            } catch (Exception e) {
                errors.add("Validation '" + validation.getName() + "' failed: " + e.getMessage());
            }
        }

        // Log warnings
        warnings.forEach(w -> log.warn("⚠️  Config warning: {}", w));

        // Fail on critical errors
        if (!errors.isEmpty()) {
            errors.forEach(e -> log.error("❌ Config error: {}", e));
            throw new ConfigurationValidationException(
                    "Configuration validation failed with " + errors.size() + " error(s)",
                    errors
            );
        }

        log.info("✅ Configuration validation passed ({} validations, {} warnings)",
                validations.size(), warnings.size());
    }
}
