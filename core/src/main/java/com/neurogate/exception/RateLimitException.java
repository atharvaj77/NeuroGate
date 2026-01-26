package com.neurogate.exception;

import java.time.Duration;

/**
 * Exception thrown when rate limit is exceeded.
 */
public class RateLimitException extends NeuroGateException {

    private final Duration retryAfter;
    private final String limitType;

    public RateLimitException(String message) {
        super(message, ErrorCode.RATE_LIMIT_EXCEEDED);
        this.retryAfter = Duration.ofSeconds(60);
        this.limitType = "requests";
    }

    public RateLimitException(String message, Duration retryAfter) {
        super(message, ErrorCode.RATE_LIMIT_EXCEEDED);
        this.retryAfter = retryAfter;
        this.limitType = "requests";
    }

    public RateLimitException(String message, Duration retryAfter, String limitType) {
        super(message, limitType.equals("budget") ? ErrorCode.BUDGET_EXCEEDED : ErrorCode.RATE_LIMIT_EXCEEDED);
        this.retryAfter = retryAfter;
        this.limitType = limitType;
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }

    public String getLimitType() {
        return limitType;
    }

    public long getRetryAfterSeconds() {
        return retryAfter.toSeconds();
    }
}
