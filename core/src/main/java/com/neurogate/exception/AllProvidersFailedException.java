package com.neurogate.exception;

import java.util.List;

/**
 * Exception thrown when all LLM providers have failed.
 */
public class AllProvidersFailedException extends NeuroGateException {

    private final List<String> attemptedProviders;

    public AllProvidersFailedException(List<String> attemptedProviders) {
        super("All LLM providers failed: " + String.join(", ", attemptedProviders),
                ErrorCode.ALL_PROVIDERS_FAILED);
        this.attemptedProviders = attemptedProviders;
    }

    public AllProvidersFailedException(String message, List<String> attemptedProviders) {
        super(message, ErrorCode.ALL_PROVIDERS_FAILED);
        this.attemptedProviders = attemptedProviders;
    }

    public List<String> getAttemptedProviders() {
        return attemptedProviders;
    }
}
