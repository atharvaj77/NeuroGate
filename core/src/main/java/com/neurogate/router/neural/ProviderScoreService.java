package com.neurogate.router.neural;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Calculates "Neural Score" for each provider.
 * Higher score = Better candidate.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderScoreService {

    private final MeterRegistry meterRegistry;

    // Cache scores: Provider Name -> Score (0.0 - 1.0)
    private final Map<String, Double> providerScores = new ConcurrentHashMap<>();

    // Weights
    private static final double WEIGHT_LATENCY = 0.4;
    private static final double WEIGHT_ERROR_RATE = 0.4;
    private static final double WEIGHT_COST = 0.2;

    @Scheduled(fixedRate = 30000) // Update every 30s
    public void updateScores() {

        List<String> providers = List.of("openai", "anthropic", "google");

        for (String provider : providers) {
            double score = calculateScore(provider);
            providerScores.put(provider, score);
        }

        log.info("Updated Neural Scores: {}", providerScores);
    }

    public double getScore(String provider) {
        return providerScores.getOrDefault(provider, 0.5); // Default neutral score
    }

    private double calculateScore(String provider) {
        // 1. Latency Score (Lower is better)
        Timer timer = meterRegistry.find("neurogate.upstream.latency")
                .tag("provider", provider)
                .timer();
        double avgLatency = (timer != null && timer.count() > 0) ? timer.mean(TimeUnit.MILLISECONDS) : 1000.0; // Default
                                                                                                               // 1s
        double latencyScore = Math.max(0, 1.0 - (avgLatency / 2000.0)); // Penalize > 2000ms

        // 2. Error Rate Score (Lower is better)
        // We look at total errors / total requests.
        // In a real system, you might want a sliding window (e.g. errors in last 5
        // min).
        // Here we use the global counter ratio for simplicity, or we could use
        // `FunctionCounter` if we had a reset mechanism.
        // Ideally, we rely on Resilience4j circuit breaker stats, but we are keeping it
        // generic via Micrometer.

        double errorCount = getCounterValue("neurogate.upstream.errors", provider);
        double requestCount = getCounterValue("neurogate.upstream.requests", provider);

        double errorRate = 0.0;
        if (requestCount > 0) {
            errorRate = errorCount / requestCount;
        }

        // Penalize errors heavily. 1% error rate = 0.95 score. 10% error = 0.5 score.
        double errorScore = Math.max(0, 1.0 - (errorRate * 5));

        // 3. Cost Score (Lower is better)
        // We track total accumulated cost and total requests to get Average Cost Per
        // Request
        double totalCost = getCounterValue("neurogate.upstream.cost", provider);
        double avgCost = (requestCount > 0) ? (totalCost / requestCount) : 0.0;

        // Benchmark: $0.01 per request is "expensive" (GPT-4), $0.001 is "cheap"
        // (GPT-3.5)
        // Score = 1.0 if free, 0.5 if $0.01/req.
        double maxAcceptableCost = 0.02; // $0.02 per request = 0 score
        double costScore = Math.max(0, 1.0 - (avgCost / maxAcceptableCost));

        log.debug(
                "Provider {}: Latency={}ms (Score={:.2f}), ErrorRate={:.2f}% (Score={:.2f}), AvgCost=${:.4f} (Score={:.2f})",
                provider, avgLatency, latencyScore, errorRate * 100, errorScore, avgCost, costScore);

        return (latencyScore * WEIGHT_LATENCY) +
                (errorScore * WEIGHT_ERROR_RATE) +
                (costScore * WEIGHT_COST);
    }

    private double getCounterValue(String name, String provider) {
        io.micrometer.core.instrument.Counter counter = meterRegistry.find(name)
                .tag("provider", provider)
                .counter();
        return counter != null ? counter.count() : 0.0;
    }
}
