package com.neurogate.router.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResilienceService {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final io.micrometer.core.instrument.MeterRegistry meterRegistry;

    /**
     * Execute a supplier with Circuit Breaker, Retry, and Latency measurement
     *
     * @param name     Name of the component/provider (e.g. "openai")
     * @param supplier The operation to execute
     * @param fallback The fallback logic to execute on failure
     * @param <T>      Return type
     * @return Result of execution or fallback
     */
    public <T> T execute(String name, Supplier<T> supplier, Function<Throwable, T> fallback) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(name);
        Retry retry = retryRegistry.retry(name);
        io.micrometer.core.instrument.Timer timer = meterRegistry.timer("neurogate.upstream.latency", "provider", name);

        Supplier<T> decoratedSupplier = CircuitBreaker.decorateSupplier(circuitBreaker, supplier);
        decoratedSupplier = Retry.decorateSupplier(retry, decoratedSupplier);

        try {
            return timer.record(decoratedSupplier);
        } catch (Throwable t) {
            log.warn("Resilience execution failed for {}: {}", name, t.getMessage());
            if (fallback != null) {
                return fallback.apply(t);
            }
            throw t;
        }
    }
}
