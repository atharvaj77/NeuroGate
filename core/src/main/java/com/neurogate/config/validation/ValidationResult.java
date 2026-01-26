package com.neurogate.config.validation;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of a configuration validation.
 */
@Data
@Builder
public class ValidationResult {

    private final boolean valid;
    private final boolean critical;
    @Builder.Default
    private final List<String> errors = new ArrayList<>();

    public static ValidationResult ok() {
        return ValidationResult.builder()
                .valid(true)
                .critical(false)
                .build();
    }

    public static ValidationResult warning(String message) {
        return ValidationResult.builder()
                .valid(false)
                .critical(false)
                .errors(List.of(message))
                .build();
    }

    public static ValidationResult error(String message) {
        return ValidationResult.builder()
                .valid(false)
                .critical(true)
                .errors(List.of(message))
                .build();
    }

    public static ValidationResult errors(List<String> messages) {
        return ValidationResult.builder()
                .valid(false)
                .critical(true)
                .errors(messages)
                .build();
    }
}
