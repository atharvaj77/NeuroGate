package com.neurogate.router.neural;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.search.Search;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProviderScoreServiceTest {

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Timer timer;

    @Mock
    private Counter counter;

    @Mock
    private Search search;

    private ProviderScoreService scoreService;

    @BeforeEach
    void setUp() {
        scoreService = new ProviderScoreService(meterRegistry);
    }

    @Test
    void testGetScore_Default() {
        assertThat(scoreService.getScore("openai")).isEqualTo(0.5);
    }

    @Test
    void testCalculateScore_IdealConditions() {
        // Mock Timer for Latency (Fast: 200ms)
        when(meterRegistry.find("neurogate.upstream.latency")).thenReturn(search);
        when(search.tag(anyString(), anyString())).thenReturn(search);
        when(search.timer()).thenReturn(timer);
        when(timer.count()).thenReturn(100L);
        when(timer.mean(TimeUnit.MILLISECONDS)).thenReturn(200.0);

        // Mock Counters for Errors (0 errors)
        // We need to handle multiple calls to find(), so we use a permissive mock setup
        // or specific args

        // Helper to mock counter
        mockCounter("neurogate.upstream.errors", 0.0);
        mockCounter("neurogate.upstream.requests", 100.0);

        // Mock Counter for Cost (Cheap: $0.001 avg)
        mockCounter("neurogate.upstream.cost", 0.1); // 0.1 / 100 = 0.001

        // Trigger update
        scoreService.updateScores();

        // Check Score
        // Latency Score: 1.0 - (200/2000) = 0.9
        // Error Score: 1.0 - (0/100 * 5) = 1.0
        // Cost Score: 1.0 - (0.001 / 0.02) = 0.95
        // Weighted: (0.9 * 0.4) + (1.0 * 0.4) + (0.95 * 0.2) = 0.36 + 0.4 + 0.19 = 0.95

        double score = scoreService.getScore("openai");
        assertThat(score).isGreaterThan(0.9);
    }

    @Test
    void testCalculateScore_HighErrorRate() {
        // Mock Latency (Medium: 1000ms)
        when(meterRegistry.find("neurogate.upstream.latency")).thenReturn(search);
        when(search.tag(anyString(), anyString())).thenReturn(search);
        when(search.timer()).thenReturn(timer);
        when(timer.count()).thenReturn(100L);
        when(timer.mean(TimeUnit.MILLISECONDS)).thenReturn(1000.0); // 0.5 score

        // Mock Counters (10% Error Rate)
        mockCounter("neurogate.upstream.errors", 10.0);
        mockCounter("neurogate.upstream.requests", 100.0); // 0.1 rate -> 0.5 score
        mockCounter("neurogate.upstream.cost", 0.1); // 0.95 score

        scoreService.updateScores();

        // Latency: 0.5
        // Error: 1.0 - (0.1 * 5) = 0.5
        // Cost: 0.95
        // Weighted: (0.5 * 0.4) + (0.5 * 0.4) + (0.95 * 0.2) = 0.2 + 0.2 + 0.19 = 0.59

        // Note: Our mock logic is a bit brittle if `find` returns same mock object for
        // different names
        // Ideally we differentiate by name args.
        // But for this unit test structure where we iterate over "openai", "anthropic",
        // "google" inside updateScores,
        // it will calculate for each. If we only assert on one provider, we can just
        // return globals.
        // However, calculateScore calls find() multiple times with DIFFERENT names.

        assertThat(scoreService.getScore("openai")).isBetween(0.5, 0.7);
    }

    private void mockCounter(String name, double value) {
        Search s = mock(Search.class);
        Counter c = mock(Counter.class);

        // This won't work cleanly because `meterRegistry.find(name)` is called inside
        // the method
        // We need `when(meterRegistry.find(eq(name))).thenReturn(s)`

        doReturn(s).when(meterRegistry).find(name);
        // doReturn(s).when(s).tag(anyString(), anyString()); // This chain is hard with
        // mocks if not careful
        // The implementation is: meterRegistry.find(name).tag(...).counter()

        // Fix:
        when(meterRegistry.find(name)).thenReturn(s);
        when(s.tag(anyString(), anyString())).thenReturn(s);
        when(s.counter()).thenReturn(c);
        when(c.count()).thenReturn(value);
    }
}
