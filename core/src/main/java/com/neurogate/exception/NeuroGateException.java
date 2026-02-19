package com.neurogate.exception;

/**
 * Base exception for all NeuroGate errors.
 * Provides consistent error handling across the application.
 */
public abstract class NeuroGateException extends RuntimeException {

    private final ErrorCode errorCode;

    protected NeuroGateException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    protected NeuroGateException(String message, Throwable cause, ErrorCode errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * Error codes for categorizing exceptions.
     */
    public enum ErrorCode {
        // Provider errors (1xxx)
        PROVIDER_UNAVAILABLE(1001),
        PROVIDER_TIMEOUT(1002),
        PROVIDER_RATE_LIMITED(1003),
        ALL_PROVIDERS_FAILED(1004),

        // Validation errors (2xxx)
        INVALID_REQUEST(2001),
        SCHEMA_VALIDATION_FAILED(2002),
        MISSING_REQUIRED_FIELD(2003),

        // Security errors (3xxx)
        SECURITY_THREAT_DETECTED(3001),
        PII_DETECTED(3002),
        JAILBREAK_ATTEMPT(3003),
        PROMPT_INJECTION(3004),
        AUTHORIZATION_FAILED(3005),

        // Rate limiting (4xxx)
        RATE_LIMIT_EXCEEDED(4001),
        BUDGET_EXCEEDED(4002),

        // Internal errors (5xxx)
        INTERNAL_ERROR(5001),
        CONFIGURATION_ERROR(5002),
        CACHE_ERROR(5003);

        private final int code;

        ErrorCode(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }
}
