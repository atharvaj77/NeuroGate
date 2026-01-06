package com.neurogate.router.resilience;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdaptiveRateLimiter {

    private final RateLimiterRegistry rateLimiterRegistry;
    private final MeterRegistry meterRegistry;

    @Value("${neurogate.resilience.latency-threshold:2000.0}")
    private double latencyThresholdMs;

    @Value("${neurogate.resilience.lower-threshold:500.0}")
    private double lowerThresholdMs;

    /**
     * Periodically checks P99 latency and adjusts rate limits.
     * Runs every 10 seconds.
     */

    @Scheduled(fixedRate = 10000)
    public void adaptLimits() {
        rateLimiterRegistry.getAllRateLimiters()
                .forEach(limiter -> adaptForProvider(limiter.getName()));
    }

    private void adaptForProvider(String provider) {
        // Ensure RateLimiter exists (or get existing)
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter(provider);

        // Get latency metric
        Timer timer = meterRegistry.find("neurogate.upstream.latency")
                .tag("provider", provider)
                .timer();

        if (timer != null) {
            double meanLatencyMs = timer.mean(java.util.concurrent.TimeUnit.MILLISECONDS);

            if (meanLatencyMs > latencyThresholdMs) {
                // Throttle down
                RateLimiterConfig config = rateLimiter.getRateLimiterConfig();
                int currentLimit = config.getLimitForPeriod();
                int newLimit = Math.max(1, (int) (currentLimit * 0.8)); // Reduce by 20%

                if (newLimit != currentLimit) {
                    log.info("High latency ({}ms) detected for {}. Throttling down to {} req/period",
                            String.format("%.2f", meanLatencyMs), provider, newLimit);
                    rateLimiter.changeLimitForPeriod(newLimit);
                }
            } else if (meanLatencyMs < lowerThresholdMs) {
                // Throttle up (recover)
                RateLimiterConfig config = rateLimiter.getRateLimiterConfig();
                int currentLimit = config.getLimitForPeriod();
                int newLimit = Math.min(1000, (int) (currentLimit * 1.2)); // Increase by 20%

                if (newLimit != currentLimit) {
                    log.info("Low latency ({}ms) detected for {}. Throttling up to {} req/period",
                            String.format("%.2f", meanLatencyMs), provider, newLimit);
                    rateLimiter.changeLimitForPeriod(newLimit);
                }
            }
        }
    }
}
