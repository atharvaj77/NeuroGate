package com.neurogate.router.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Chains multiple routing strategies together.
 * Strategies are applied in priority order until one succeeds.
 */
@Slf4j
@Component
public class RoutingStrategyChain {

    private final List<RoutingStrategy> strategies;

    public RoutingStrategyChain(List<RoutingStrategy> strategies) {
        this.strategies = strategies.stream()
                .sorted(Comparator.comparingInt(RoutingStrategy::getPriority))
                .toList();

        log.info("Initialized routing strategy chain with {} strategies: {}",
                this.strategies.size(),
                this.strategies.stream().map(RoutingStrategy::getName).toList());
    }

    /**
     * Apply all enabled strategies in priority order.
     *
     * @param context the initial routing context
     * @return the final routing context after all strategies applied
     */
    public RoutingContext apply(RoutingContext context) {
        RoutingContext current = context;

        for (RoutingStrategy strategy : strategies) {
            if (!strategy.isEnabled()) {
                log.trace("Skipping disabled strategy: {}", strategy.getName());
                continue;
            }

            try {
                Optional<RoutingContext> result = strategy.apply(current);
                if (result.isPresent()) {
                    log.debug("Strategy '{}' applied: {} → {}",
                            strategy.getName(),
                            current.getSelectedModel(),
                            result.get().getSelectedModel());
                    current = result.get();
                }
            } catch (Exception e) {
                log.warn("Strategy '{}' failed: {}", strategy.getName(), e.getMessage());
                // Continue with next strategy
            }
        }

        return current;
    }

    /**
     * Get all registered strategies.
     */
    public List<RoutingStrategy> getStrategies() {
        return strategies;
    }
}
