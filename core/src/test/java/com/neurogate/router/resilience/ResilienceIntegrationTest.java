package com.neurogate.router.resilience;

import com.neurogate.sentinel.model.ChatRequest;
import com.neurogate.sentinel.model.ChatResponse;
import com.neurogate.sentinel.model.Message;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = ResilienceService.class)
@org.springframework.test.context.ActiveProfiles("test")
@org.springframework.boot.autoconfigure.EnableAutoConfiguration
@org.springframework.boot.autoconfigure.ImportAutoConfiguration(classes = {
        io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration.class,
        io.github.resilience4j.springboot3.retry.autoconfigure.RetryAutoConfiguration.class
})
class ResilienceIntegrationTest {

    @Autowired
    private ResilienceService resilienceService;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Test
    @DisplayName("Circuit Breaker - Should open after failures and trigger fallback")
    void testCircuitBreakerOpens() {
        String componentName = "test-provider";

        // Programmatic configuration to ensure test isolation and correctness
        io.github.resilience4j.circuitbreaker.CircuitBreakerConfig config = io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
                .custom()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50.0f)
                .waitDurationInOpenState(java.time.Duration.ofSeconds(60))
                .permittedNumberOfCallsInHalfOpenState(3)
                .build();

        // Ensure strictly new instance with our config
        circuitBreakerRegistry.remove(componentName);
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(componentName, config);

        // Reset state
        circuitBreaker.reset();

        AtomicInteger callCount = new AtomicInteger(0);
        Supplier<String> failingSupplier = () -> {
            callCount.incrementAndGet();
            throw new RuntimeException("Simulated Failure");
        };
        Function<Throwable, String> fallback = (t) -> "Fallback Response";

        for (int i = 0; i < 20; i++) {
            String result = resilienceService.execute(componentName, failingSupplier, fallback);
            assertEquals("Fallback Response", result);
        }

        // Verify Circuit Breaker is OPEN
        System.out.println("DEBUG: State before check: " + circuitBreaker.getState());
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());

        // 2. Next call should fail fast (CallNotPermittedException) wrapped in fallback
        // The supplier should NOT be called
        int callsBefore = callCount.get();
        System.out.println("DEBUG: Calls before: " + callsBefore);

        String result = resilienceService.execute(componentName, failingSupplier, fallback);

        int callsAfter = callCount.get();
        System.out.println("DEBUG: Calls after: " + callsAfter);

        assertEquals(callsBefore, callsAfter, "Supplier should not be called when CB is Open");
        assertEquals("Fallback Response", result);
    }

    @Test
    @DisplayName("Circuit Breaker - Should recover after wait duration (Half-Open)")
    void testCircuitBreakerRecovery() {
        String componentName = "test-recovery";
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(componentName);
        circuitBreaker.transitionToOpenState();
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());
        // Transition to HALF_OPEN
        circuitBreaker.transitionToHalfOpenState();
        assertEquals(CircuitBreaker.State.HALF_OPEN, circuitBreaker.getState());

        // We need 'permittedNumberOfCallsInHalfOpenState' successful calls to close the
        // circuit.
        // Default is usually 10. We'll try 20 to be safe, or check status.
        for (int i = 0; i < 20; i++) {
            resilienceService.execute(componentName, () -> "Success", t -> "Fallback");
            if (circuitBreaker.getState() == CircuitBreaker.State.CLOSED) {
                break;
            }
        }

        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
    }
}
