package com.neurogate.router.strategy;

import com.neurogate.sentinel.model.ChatRequest;

import java.util.Optional;

/**
 * Strategy interface for request routing decisions.
 * Implements the Strategy Pattern for flexible routing logic.
 */
public interface RoutingStrategy {

    /**
     * Attempt to apply this routing strategy to the request.
     *
     * @param context the routing context containing request and metadata
     * @return Optional containing modified context if strategy applied, empty otherwise
     */
    Optional<RoutingContext> apply(RoutingContext context);

    /**
     * Get the priority of this strategy (lower = higher priority).
     */
    int getPriority();

    /**
     * Get the name of this strategy for logging/metrics.
     */
    String getName();

    /**
     * Check if this strategy is enabled.
     */
    default boolean isEnabled() {
        return true;
    }
}
