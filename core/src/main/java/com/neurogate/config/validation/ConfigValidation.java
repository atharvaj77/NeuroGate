package com.neurogate.config.validation;

/**
 * Interface for configuration validations.
 */
public interface ConfigValidation {

    /**
     * Perform validation and return result.
     */
    ValidationResult validate();

    /**
     * Get the name of this validation.
     */
    String getName();
}
