package com.neurogate.config.validation;

import java.util.List;

/**
 * Exception thrown when configuration validation fails.
 */
public class ConfigurationValidationException extends RuntimeException {

    private final List<String> errors;

    public ConfigurationValidationException(String message, List<String> errors) {
        super(message + ": " + String.join(", ", errors));
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}
