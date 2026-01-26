package com.neurogate.validation.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single JSON schema validation error.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationError {

    /**
     * JSON path where the error occurred (e.g., "$.name", "$.items[0].price").
     */
    private String path;

    /**
     * Human-readable error message.
     */
    private String message;

    /**
     * Type of validation failure.
     */
    private String errorType;

    /**
     * Expected value or schema.
     */
    private String expected;

    /**
     * Actual value found.
     */
    private String actual;

    /**
     * Create a simple error with path and message.
     */
    public static ValidationError of(String path, String message) {
        return ValidationError.builder()
                .path(path)
                .message(message)
                .build();
    }

    /**
     * Create a type mismatch error.
     */
    public static ValidationError typeMismatch(String path, String expected, String actual) {
        return ValidationError.builder()
                .path(path)
                .message("Type mismatch: expected " + expected + ", got " + actual)
                .errorType("TYPE_MISMATCH")
                .expected(expected)
                .actual(actual)
                .build();
    }

    /**
     * Create a required field error.
     */
    public static ValidationError required(String path) {
        return ValidationError.builder()
                .path(path)
                .message("Required field is missing")
                .errorType("REQUIRED")
                .build();
    }
}