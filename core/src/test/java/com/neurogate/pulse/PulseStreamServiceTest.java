package com.neurogate.pulse;

import com.neurogate.pulse.dto.MetricsSnapshot;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link PulseStreamService} rolling-metrics computation.
 */
class PulseStreamServiceTest {

    private PulseStreamService service;

    @BeforeEach
    void setUp() {
        PulseEventPublisher publisher = Mockito.mock(PulseEventPublisher.class);
        Mockito.when(publisher.getConnectedClientCount()).thenReturn(0);
        service = new PulseStreamService(publisher, new SimpleMeterRegistry());
    }

    @Test
    void emptyWindow_returnsZeroMetrics() {
        MetricsSnapshot snap = service.getCurrentSnapshot();
        assertThat(snap).isNotNull();
        assertThat(snap.getRps()).isEqualTo(0.0);
        assertThat(snap.getAvgLatencyMs()).isEqualTo(0.0);
        assertThat(snap.getErrorRate()).isEqualTo(0.0);
    }

    @Test
    void singleRequest_computesCorrectAverage() {
        service.recordRequest(200, false, false, false, 100, "openai", 0.01);

        MetricsSnapshot snap = service.getCurrentSnapshot();
        assertThat(snap.getAvgLatencyMs()).isEqualTo(200.0);
        assertThat(snap.getP95LatencyMs()).isEqualTo(200.0);
        assertThat(snap.getErrorRate()).isEqualTo(0.0);
        assertThat(snap.getTokenCount()).isEqualTo(100);
        assertThat(snap.getCostUsdTotal()).isEqualTo(0.01);
        assertThat(snap.getActiveProvider()).isEqualTo("openai");
    }

    @Test
    void errorRequestsUpdateErrorRate() {
        // 1 error out of 2 requests = 50% error rate
        service.recordRequest(100, false, false, false, 50, "openai", 0.005);
        service.recordRequest(150, true, false, false, 0, "openai", 0.0);

        MetricsSnapshot snap = service.getCurrentSnapshot();
        assertThat(snap.getErrorRate()).isEqualTo(0.5);
    }

    @Test
    void cacheHitUpdatesHitRate() {
        service.recordRequest(10, false, true, false, 0, "cache", 0.0);
        service.recordRequest(200, false, false, false, 100, "openai", 0.01);

        MetricsSnapshot snap = service.getCurrentSnapshot();
        assertThat(snap.getCacheHitRate()).isEqualTo(0.5);
    }

    @Test
    void piiDetectedIncrementsPiiBlocked() {
        service.recordRequest(100, false, false, true, 80, "openai", 0.008);

        MetricsSnapshot snap = service.getCurrentSnapshot();
        assertThat(snap.getPiiBlocked()).isEqualTo(1L);
    }

    @Test
    void broadcastMetrics_withNoClients_doesNotThrow() {
        // Should return early without publishing
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                () -> service.broadcastMetrics());
    }
}
