package com.neurogate.exception;

/**
 * Exception thrown when an LLM provider fails.
 */
public class ProviderException extends NeuroGateException {

    private final String provider;
    private final int statusCode;

    public ProviderException(String provider, String message) {
        super(message, ErrorCode.PROVIDER_UNAVAILABLE);
        this.provider = provider;
        this.statusCode = 500;
    }

    public ProviderException(String provider, String message, int statusCode) {
        super(message, determineErrorCode(statusCode));
        this.provider = provider;
        this.statusCode = statusCode;
    }

    public ProviderException(String provider, String message, Throwable cause) {
        super(message, cause, ErrorCode.PROVIDER_UNAVAILABLE);
        this.provider = provider;
        this.statusCode = 500;
    }

    private static ErrorCode determineErrorCode(int statusCode) {
        return switch (statusCode) {
            case 429 -> ErrorCode.PROVIDER_RATE_LIMITED;
            case 408, 504 -> ErrorCode.PROVIDER_TIMEOUT;
            default -> ErrorCode.PROVIDER_UNAVAILABLE;
        };
    }

    public String getProvider() {
        return provider;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
