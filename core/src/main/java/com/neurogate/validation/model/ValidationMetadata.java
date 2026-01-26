package com.neurogate.validation.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Validation metadata included in ChatResponse.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationMetadata {

    /**
     * Whether the response is valid JSON matching the schema.
     */
    private boolean schemaValid;

    /**
     * Number of retries needed to get valid output.
     */
    private int retriesNeeded;

    /**
     * Whether auto-fix was applied to correct minor issues.
     */
    private boolean autoFixed;

    /**
     * Validation errors (if schema invalid).
     */
    private List<ValidationError> errors;

    /**
     * Create metadata for valid response.
     */
    public static ValidationMetadata valid(int retries) {
        return ValidationMetadata.builder()
                .schemaValid(true)
                .retriesNeeded(retries)
                .build();
    }

    /**
     * Create metadata for invalid response.
     */
    public static ValidationMetadata invalid(int retries, List<ValidationError> errors) {
        return ValidationMetadata.builder()
                .schemaValid(false)
                .retriesNeeded(retries)
                .errors(errors)
                .build();
    }
}