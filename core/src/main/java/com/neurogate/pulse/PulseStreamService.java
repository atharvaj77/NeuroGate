package com.neurogate.pulse;

import com.neurogate.pulse.dto.MetricsSnapshot;
import com.neurogate.pulse.model.PulseEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

/**
 * PulseStreamService — computes real-time rolling metrics from gateway traffic
 * and broadcasts them to all connected Pulse Dashboard clients every second.
 *
 * <h2>Rolling Window Strategy</h2>
 * Latency samples are stored in a {@code long[] latencyRingBuffer} of the last
 * WINDOW_SIZE measurements. RPS is derived from a 10-second request counter.
 * Error rate, cache hit rate are computed over the same window. All state is
 * updated lock-free using {@link LongAdder} / {@link AtomicLong}.
 *
 * <h2>Prometheus Metrics</h2>
 * Custom named metrics are registered with Micrometer and exposed via
 * {@code /actuator/prometheus}:
 * <ul>
 * <li>{@code neurogate_requests_total}</li>
 * <li>{@code neurogate_cache_hits_total}</li>
 * <li>{@code neurogate_pii_detections_total}</li>
 * <li>{@code neurogate_provider_errors_total}</li>
 * <li>{@code neurogate_tokens_total}</li>
 * <li>{@code neurogate_cost_usd_total} (gauge)</li>
 * </ul>
 */
@Slf4j
@Service
public class PulseStreamService {

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    /** Number of latency samples kept in the rolling window. */
    private static final int WINDOW_SIZE = 600; // ~10 minutes at 1 req/sec

    /** Number of seconds used for RPS calculation. */
    private static final int RPS_WINDOW_SECONDS = 10;

    // -----------------------------------------------------------------------
    // Dependencies
    // -----------------------------------------------------------------------

    private final PulseEventPublisher eventPublisher;
    private final MeterRegistry meterRegistry;

    // -----------------------------------------------------------------------
    // Rolling-window state (lock-free)
    // -----------------------------------------------------------------------

    /** Circular buffer of recent latency samples (milliseconds). */
    private final long[] latencyRingBuffer = new long[WINDOW_SIZE];
    private final AtomicLong ringHead = new AtomicLong(0);
    private final AtomicLong ringSamples = new AtomicLong(0);

    /** Requests received in each of the last RPS_WINDOW_SECONDS seconds. */
    private final long[] rpsWindow = new long[RPS_WINDOW_SECONDS];
    private volatile int currentRpsSlot = 0;
    private volatile long lastRpsFlipSecond = System.currentTimeMillis() / 1000;

    /** Total error count in the rolling window (same size as latency ring). */
    private final long[] errorWindow = new long[WINDOW_SIZE];

    /** Total cache-hit count in the rolling window. */
    private final long[] cacheHitWindow = new long[WINDOW_SIZE];

    // -----------------------------------------------------------------------
    // Cumulative counters
    // -----------------------------------------------------------------------

    private final LongAdder totalTokens = new LongAdder();
    private final LongAdder totalPiiBlocks = new LongAdder();
    private final AtomicReference<String> lastProvider = new AtomicReference<>("openai");
    private final AtomicReference<Double> totalCostUsd = new AtomicReference<>(0.0);

    // -----------------------------------------------------------------------
    // Micrometer custom metric handles
    // -----------------------------------------------------------------------

    private final Counter requestsTotal;
    private final Counter cacheHitsTotal;
    private final Counter piiDetectionsTotal;
    private final Counter providerErrorsTotal;
    private final Counter tokensTotal;

    // -----------------------------------------------------------------------
    // Constructor — register Prometheus metrics
    // -----------------------------------------------------------------------

    public PulseStreamService(PulseEventPublisher eventPublisher, MeterRegistry meterRegistry) {
        this.eventPublisher = eventPublisher;
        this.meterRegistry = meterRegistry;

        this.requestsTotal = Counter.builder("neurogate_requests_total")
                .description("Total requests processed by NeuroGate")
                .register(meterRegistry);

        this.cacheHitsTotal = Counter.builder("neurogate_cache_hits_total")
                .description("Total cache hits (any tier)")
                .register(meterRegistry);

        this.piiDetectionsTotal = Counter.builder("neurogate_pii_detections_total")
                .description("Total PII detection events")
                .register(meterRegistry);

        this.providerErrorsTotal = Counter.builder("neurogate_provider_errors_total")
                .description("Total upstream provider errors")
                .register(meterRegistry);

        this.tokensTotal = Counter.builder("neurogate_tokens_total")
                .description("Total tokens consumed")
                .register(meterRegistry);

        Gauge.builder("neurogate_cost_usd_total", totalCostUsd, AtomicReference::get)
                .description("Cumulative LLM cost in USD since service start")
                .register(meterRegistry);
    }

    // -----------------------------------------------------------------------
    // Public API — called by gateway filters/services
    // -----------------------------------------------------------------------

    /**
     * Record a completed gateway request. Thread-safe, called from multiple
     * request-processing threads.
     *
     * @param latencyMs   end-to-end latency in milliseconds
     * @param isError     whether the request resulted in an upstream error
     * @param cacheHit    whether served from cache
     * @param piiDetected whether PII was detected in this request
     * @param tokens      total tokens consumed (0 for cache hits)
     * @param provider    provider name ("openai", "anthropic", etc.)
     * @param costUsd     cost of this request in USD
     */
    public void recordRequest(long latencyMs, boolean isError, boolean cacheHit,
            boolean piiDetected, int tokens, String provider, double costUsd) {
        // Advance ring index
        long idx = ringHead.getAndIncrement() % WINDOW_SIZE;
        if (ringSamples.get() < WINDOW_SIZE)
            ringSamples.incrementAndGet();

        // Store in ring buffers
        latencyRingBuffer[(int) idx] = latencyMs;
        errorWindow[(int) idx] = isError ? 1 : 0;
        cacheHitWindow[(int) idx] = cacheHit ? 1 : 0;

        // Update cumulative state
        totalTokens.add(tokens);
        if (piiDetected)
            totalPiiBlocks.increment();
        lastProvider.set(provider != null ? provider : "unknown");
        totalCostUsd.getAndUpdate(prev -> prev + costUsd);

        // Update RPS window
        advanceRpsWindowIfNeeded();
        synchronized (rpsWindow) {
            rpsWindow[currentRpsSlot]++;
        }

        // Update Prometheus counters
        requestsTotal.increment();
        if (cacheHit)
            cacheHitsTotal.increment();
        if (piiDetected)
            piiDetectionsTotal.increment();
        if (isError)
            providerErrorsTotal.increment();
        if (tokens > 0)
            tokensTotal.increment(tokens);
    }

    // -----------------------------------------------------------------------
    // Public snapshot accessor
    // -----------------------------------------------------------------------

    /**
     * Returns the current rolling-window snapshot without broadcasting.
     * Used by {@link com.neurogate.pulse.history.MetricsHistoryService} and
     * {@link com.neurogate.alert.AlertService}.
     */
    public MetricsSnapshot getCurrentSnapshot() {
        return buildSnapshot();
    }

    // -----------------------------------------------------------------------
    // Scheduled broadcast
    // -----------------------------------------------------------------------

    /**
     * Computes the current {@link MetricsSnapshot} from the rolling window and
     * broadcasts it to all connected WebSocket clients every second.
     */
    @Scheduled(fixedRate = 1000)
    public void broadcastMetrics() {
        if (eventPublisher.getConnectedClientCount() == 0) {
            return;
        }

        try {
            MetricsSnapshot snapshot = buildSnapshot();

            PulseEvent event = PulseEvent.builder()
                    .type(PulseEvent.EventType.METRIC_UPDATE)
                    .timestamp(Instant.now())
                    .payload(snapshot)
                    .build();

            eventPublisher.publish(event);
        } catch (Exception e) {
            log.error("Error broadcasting pulse metrics", e);
        }
    }

    // -----------------------------------------------------------------------
    // Snapshot computation
    // -----------------------------------------------------------------------

    private MetricsSnapshot buildSnapshot() {
        long samples = Math.min(ringSamples.get(), WINDOW_SIZE);

        double avgLatency = 0.0;
        double p95Latency = 0.0;
        double errorRate = 0.0;
        double cacheHitRate = 0.0;

        if (samples > 0) {
            long head = ringHead.get();
            long[] latencies = new long[(int) samples];
            long errorCount = 0;
            long cacheCount = 0;

            for (int i = 0; i < samples; i++) {
                int idx = (int) ((head - samples + i + WINDOW_SIZE) % WINDOW_SIZE);
                latencies[i] = latencyRingBuffer[idx];
                errorCount += errorWindow[idx];
                cacheCount += cacheHitWindow[idx];
            }

            // Average latency
            long sum = 0;
            for (long l : latencies)
                sum += l;
            avgLatency = (double) sum / samples;

            // P95 latency
            long[] sorted = Arrays.copyOf(latencies, (int) samples);
            Arrays.sort(sorted);
            int p95Idx = (int) Math.ceil(0.95 * sorted.length) - 1;
            p95Latency = sorted[Math.max(0, p95Idx)];

            errorRate = (double) errorCount / samples;
            cacheHitRate = (double) cacheCount / samples;
        }

        return MetricsSnapshot.builder()
                .timestamp(Instant.now())
                .rps(computeRps())
                .avgLatencyMs(avgLatency)
                .p95LatencyMs(p95Latency)
                .errorRate(errorRate)
                .tokenCount(totalTokens.sum())
                .cacheHitRate(cacheHitRate)
                .piiBlocked(totalPiiBlocks.sum())
                .activeProvider(lastProvider.get())
                .costUsdTotal(totalCostUsd.get())
                .build();
    }

    private double computeRps() {
        advanceRpsWindowIfNeeded();
        synchronized (rpsWindow) {
            long total = 0;
            for (long v : rpsWindow)
                total += v;
            return (double) total / RPS_WINDOW_SECONDS;
        }
    }

    /**
     * Advances the RPS window slot when the current second has elapsed.
     * Zeroes out intervening slots to handle gaps in traffic.
     */
    private void advanceRpsWindowIfNeeded() {
        long nowSecond = System.currentTimeMillis() / 1000;
        long lastSecond = lastRpsFlipSecond;
        if (nowSecond <= lastSecond)
            return;

        synchronized (rpsWindow) {
            // Double-check inside lock
            if (nowSecond <= lastRpsFlipSecond)
                return;
            long gap = Math.min(nowSecond - lastRpsFlipSecond, RPS_WINDOW_SECONDS);
            for (long i = 1; i <= gap; i++) {
                int slot = (int) ((currentRpsSlot + i) % RPS_WINDOW_SECONDS);
                rpsWindow[slot] = 0;
            }
            currentRpsSlot = (int) (nowSecond % RPS_WINDOW_SECONDS);
            lastRpsFlipSecond = nowSecond;
        }
    }
}
