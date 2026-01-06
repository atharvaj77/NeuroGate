package com.neurogate.router.resilience;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.search.Search;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdaptiveRateLimiterTest {

    @Mock
    private RateLimiterRegistry rateLimiterRegistry;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private RateLimiter rateLimiter;

    @Mock
    private RateLimiterConfig rateLimiterConfig;

    @Mock
    private Timer timer;

    @InjectMocks
    private AdaptiveRateLimiter adaptiveRateLimiter;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adaptiveRateLimiter, "latencyThresholdMs", 2000.0);
        ReflectionTestUtils.setField(adaptiveRateLimiter, "lowerThresholdMs", 500.0);
    }

    @Test
    void adaptLimits_ShouldThrottleDown_WhenLatencyHigh() {
        // Arrange
        String provider = "openai";
        when(rateLimiterRegistry.getAllRateLimiters()).thenReturn(java.util.Set.of(rateLimiter));
        when(rateLimiter.getName()).thenReturn(provider);
        when(rateLimiterRegistry.rateLimiter(provider)).thenReturn(rateLimiter);

        // Mock Timer search
        Search search = mock(Search.class);
        when(meterRegistry.find("neurogate.upstream.latency")).thenReturn(search);
        when(search.tag("provider", provider)).thenReturn(search);
        when(search.timer()).thenReturn(timer);

        // Mock High Latency (3000ms > 2000ms)
        when(timer.mean(TimeUnit.MILLISECONDS)).thenReturn(3000.0);

        // Mock Current Limits
        when(rateLimiter.getRateLimiterConfig()).thenReturn(rateLimiterConfig);
        when(rateLimiterConfig.getLimitForPeriod()).thenReturn(100);

        // Act
        adaptiveRateLimiter.adaptLimits();

        // Assert
        // Should reduce by 20% -> 80
        verify(rateLimiter).changeLimitForPeriod(80);
    }

    @Test
    void adaptLimits_ShouldThrottleUp_WhenLatencyLow() {
        // Arrange
        String provider = "anthropic";
        when(rateLimiterRegistry.getAllRateLimiters()).thenReturn(java.util.Set.of(rateLimiter));
        when(rateLimiter.getName()).thenReturn(provider);
        when(rateLimiterRegistry.rateLimiter(provider)).thenReturn(rateLimiter);

        Search search = mock(Search.class);
        when(meterRegistry.find("neurogate.upstream.latency")).thenReturn(search);
        when(search.tag("provider", provider)).thenReturn(search);
        when(search.timer()).thenReturn(timer);

        // Mock Low Latency (100ms < 500ms)
        when(timer.mean(TimeUnit.MILLISECONDS)).thenReturn(100.0);

        when(rateLimiter.getRateLimiterConfig()).thenReturn(rateLimiterConfig);
        when(rateLimiterConfig.getLimitForPeriod()).thenReturn(100);

        // Act
        adaptiveRateLimiter.adaptLimits();

        // Assert
        // Should increase by 20% -> 120
        verify(rateLimiter).changeLimitForPeriod(120);
    }

    @Test
    void adaptLimits_ShouldDoNothing_WhenLatencyNormal() {
        // Arrange
        String provider = "gemini";
        when(rateLimiterRegistry.getAllRateLimiters()).thenReturn(java.util.Set.of(rateLimiter));
        when(rateLimiter.getName()).thenReturn(provider);
        when(rateLimiterRegistry.rateLimiter(provider)).thenReturn(rateLimiter);

        Search search = mock(Search.class);
        when(meterRegistry.find("neurogate.upstream.latency")).thenReturn(search);
        when(search.tag("provider", provider)).thenReturn(search);
        when(search.timer()).thenReturn(timer);

        // Mock Normal Latency (1000ms)
        when(timer.mean(TimeUnit.MILLISECONDS)).thenReturn(1000.0);

        // Act
        adaptiveRateLimiter.adaptLimits();

        // Assert
        verify(rateLimiter, never()).changeLimitForPeriod(anyInt());
    }
}
