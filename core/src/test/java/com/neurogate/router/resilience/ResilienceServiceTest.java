package com.neurogate.router.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ResilienceServiceTest {

    private ResilienceService resilienceService;
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void setUp() {
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .minimumNumberOfCalls(2)
                .slidingWindowSize(5)
                .waitDurationInOpenState(Duration.ofSeconds(1))
                .build();
        circuitBreakerRegistry = CircuitBreakerRegistry.of(cbConfig);

        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(100))
                .build();
        RetryRegistry retryRegistry = RetryRegistry.of(retryConfig);
        io.micrometer.core.instrument.MeterRegistry meterRegistry = new io.micrometer.core.instrument.simple.SimpleMeterRegistry();

        resilienceService = new ResilienceService(circuitBreakerRegistry, retryRegistry, meterRegistry);
    }

    @Test
    void testExecute_Success() {
        String result = resilienceService.execute(
                "test-provider",
                () -> "Success",
                throwable -> "Fallback");

        assertEquals("Success", result);
    }

    @Test
    void testExecute_TriggersRetry() {
        AtomicInteger attempts = new AtomicInteger(0);

        String result = resilienceService.execute(
                "retry-test-provider",
                () -> {
                    if (attempts.incrementAndGet() < 2) {
                        throw new RuntimeException("Transient failure");
                    }
                    return "Success after retries";
                },
                throwable -> "Fallback");

        assertEquals("Success after retries", result);
        assertEquals(2, attempts.get());
    }

    @Test
    void testExecute_FallbackOnFailure() {
        String result = resilienceService.execute(
                "test-provider",
                () -> {
                    throw new RuntimeException("Always fails");
                },
                throwable -> "Fallback executed");

        assertEquals("Fallback executed", result);
    }

    @Test
    void testCircuitBreaker_OpensAfterFailures() {
        // Fail enough times to open the circuit
        for (int i = 0; i < 5; i++) {
            resilienceService.execute(
                    "flaky-provider",
                    () -> {
                        throw new RuntimeException("Fail");
                    },
                    throwable -> "Fallback");
        }

        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("flaky-provider");
        assertEquals(CircuitBreaker.State.OPEN, cb.getState());
    }
}
