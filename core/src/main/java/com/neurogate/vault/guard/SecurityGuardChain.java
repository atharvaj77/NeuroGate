package com.neurogate.vault.guard;

import com.neurogate.vault.neuroguard.model.ThreatDetectionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Chain of Responsibility implementation for security guards.
 * Executes all guards in priority order and returns the highest threat result.
 */
@Slf4j
@Component
public class SecurityGuardChain {

    private final List<SecurityGuard> guards;
    private final AtomicLong totalScans = new AtomicLong(0);
    private final AtomicLong threatsDetected = new AtomicLong(0);
    private final Map<SecurityGuard.GuardType, AtomicLong> threatsByType = new ConcurrentHashMap<>();

    public SecurityGuardChain(List<SecurityGuard> guards) {
        this.guards = new ArrayList<>(guards);
        this.guards.sort(Comparator.comparingInt(SecurityGuard::getPriority));
        log.info("Security guard chain initialized with {} guards: {}",
                guards.size(),
                guards.stream().map(g -> g.getType().name()).toList());
    }

    /**
     * Execute the guard chain on the given content.
     *
     * @param content The content to analyze
     * @return The highest confidence threat result, or safe if no threats
     */
    public ThreatDetectionResult execute(String content) {
        totalScans.incrementAndGet();

        ThreatDetectionResult highestThreat = ThreatDetectionResult.safe();

        for (SecurityGuard guard : guards) {
            try {
                ThreatDetectionResult result = guard.check(content);

                if (result.isThreatDetected()) {
                    log.debug("Guard {} detected threat: {} (confidence: {})",
                            guard.getType(), result.getThreatType(), result.getConfidenceScore());

                    threatsByType.computeIfAbsent(guard.getType(), k -> new AtomicLong(0))
                            .incrementAndGet();

                    // Keep the highest confidence threat
                    if (result.getConfidenceScore() > highestThreat.getConfidenceScore()) {
                        highestThreat = result;
                    }

                    // If blocked, stop the chain immediately
                    if (result.isBlocked()) {
                        log.warn("Guard {} blocked content: {}", guard.getType(), result.getMessage());
                        threatsDetected.incrementAndGet();
                        return result;
                    }
                }
            } catch (Exception e) {
                log.error("Guard {} threw exception: {}", guard.getType(), e.getMessage(), e);
                // Continue with other guards
            }
        }

        if (highestThreat.isThreatDetected()) {
            threatsDetected.incrementAndGet();
        }

        return highestThreat;
    }

    /**
     * Get statistics about guard chain execution.
     */
    public Map<String, Object> getStatistics() {
        Map<String, Long> threatCounts = new ConcurrentHashMap<>();
        threatsByType.forEach((type, count) -> threatCounts.put(type.name(), count.get()));

        return Map.of(
                "total_scans", totalScans.get(),
                "threats_detected", threatsDetected.get(),
                "threats_by_guard", threatCounts,
                "guard_count", guards.size()
        );
    }

    /**
     * Add a guard dynamically (e.g., for custom guards).
     */
    public void addGuard(SecurityGuard guard) {
        guards.add(guard);
        guards.sort(Comparator.comparingInt(SecurityGuard::getPriority));
        log.info("Added guard {} to chain", guard.getType());
    }

    /**
     * Remove a guard by type.
     */
    public void removeGuard(SecurityGuard.GuardType type) {
        guards.removeIf(g -> g.getType() == type);
        log.info("Removed guard {} from chain", type);
    }

    /**
     * Builder for creating a guard chain.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final List<SecurityGuard> guards = new ArrayList<>();

        public Builder add(SecurityGuard guard) {
            guards.add(guard);
            return this;
        }

        public SecurityGuardChain build() {
            return new SecurityGuardChain(guards);
        }
    }
}
