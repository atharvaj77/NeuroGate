package com.neurogate.exception;

import java.util.List;

/**
 * Exception thrown when request or response validation fails.
 */
public class ValidationException extends NeuroGateException {

    private final List<String> errors;

    public ValidationException(String message) {
        super(message, ErrorCode.INVALID_REQUEST);
        this.errors = List.of(message);
    }

    public ValidationException(List<String> errors) {
        super("Validation failed: " + String.join(", ", errors), ErrorCode.INVALID_REQUEST);
        this.errors = errors;
    }

    public ValidationException(String message, ErrorCode errorCode) {
        super(message, errorCode);
        this.errors = List.of(message);
    }

    public List<String> getErrors() {
        return errors;
    }
}
