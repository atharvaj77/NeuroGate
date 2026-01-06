package com.neurogate.pulse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neurogate.pulse.model.PulseEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PulseStreamService {

    private final MeterRegistry meterRegistry;
    private final PulseEventPublisher eventPublisher;

    /**
     * Pushes real-time metrics to all connected clients every second.
     */
    @Scheduled(fixedRate = 1000)
    public void broadcastMetrics() {
        if (eventPublisher.getConnectedClientCount() == 0) {
            return;
        }

        try {
            PulseMetrics metrics = collectMetrics();

            PulseEvent event = PulseEvent.builder()
                    .type(PulseEvent.EventType.METRIC_UPDATE)
                    .timestamp(Instant.now())
                    .payload(metrics)
                    .build();

            eventPublisher.publish(event);
        } catch (Exception e) {
            log.error("Error broadcasting pulse metrics", e);
        }
    }

    private PulseMetrics collectMetrics() {
        // Collect metrics from Micrometer
        Timer timer = meterRegistry.find("neurogate.upstream.latency")
                .tag("provider", "openai") // Example
                .timer();

        double avgLatency = (timer != null) ? timer.mean(TimeUnit.MILLISECONDS) : 0.0;
        long totalRequests = (timer != null) ? timer.count() : 0;

        return new PulseMetrics(
                System.currentTimeMillis(),
                Map.of("openai", new PulseMetrics.ProviderMetrics(avgLatency, totalRequests)));
    }

    // Tiny DTOs
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class PulseMetrics {
        private long timestamp;
        private Map<String, ProviderMetrics> providers;

        @lombok.Data
        @lombok.AllArgsConstructor
        public static class ProviderMetrics {
            private double latencyMs;
            private long totalRequests;
        }
    }
}
