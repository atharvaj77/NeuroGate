package com.neurogate.router.stream;

import com.neurogate.sentinel.model.ChatResponse;
import reactor.core.publisher.Flux;

/**
 * Processor interface for stream transformations.
 * Implements the Decorator Pattern for streaming responses.
 */
public interface StreamProcessor {

    /**
     * Process a stream of chat responses.
     *
     * @param stream the input stream
     * @return the processed stream
     */
    Flux<ChatResponse> process(Flux<ChatResponse> stream);

    /**
     * Get the priority of this processor (lower = runs first).
     */
    int getPriority();

    /**
     * Get the name of this processor for logging.
     */
    String getName();

    /**
     * Check if this processor is enabled.
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * Reset any internal state (called before each new stream).
     */
    default void reset() {
        // Default no-op
    }
}
