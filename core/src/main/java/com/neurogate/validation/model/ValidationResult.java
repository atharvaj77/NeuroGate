package com.neurogate.validation.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Result of JSON schema validation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {

    /**
     * Whether the JSON is valid according to the schema.
     */
    private boolean valid;

    /**
     * The original response content.
     */
    private String originalContent;

    /**
     * Cleaned/fixed content (if auto-fix was applied).
     */
    private String fixedContent;

    /**
     * Whether auto-fix was applied.
     */
    private boolean autoFixed;

    /**
     * List of validation errors (empty if valid).
     */
    private List<ValidationError> errors;

    /**
     * Create a valid result.
     */
    public static ValidationResult valid(String content) {
        return ValidationResult.builder()
                .valid(true)
                .originalContent(content)
                .errors(List.of())
                .build();
    }

    /**
     * Create an invalid result with errors.
     */
    public static ValidationResult invalid(String content, List<ValidationError> errors) {
        return ValidationResult.builder()
                .valid(false)
                .originalContent(content)
                .errors(errors)
                .build();
    }

    /**
     * Create a result with auto-fix applied.
     */
    public static ValidationResult autoFixed(String original, String fixed) {
        return ValidationResult.builder()
                .valid(true)
                .originalContent(original)
                .fixedContent(fixed)
                .autoFixed(true)
                .errors(List.of())
                .build();
    }

    /**
     * Check if there are any errors.
     */
    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }
}