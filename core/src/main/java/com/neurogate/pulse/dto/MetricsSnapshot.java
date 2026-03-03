package com.neurogate.pulse.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * MetricsSnapshot — aggregated real-time metrics broadcast to Pulse Dashboard
 * clients every second via WebSocket.
 *
 * <p>
 * All rate/count values are computed over a 1-second rolling window (or
 * since the last snapshot for cumulative counters).
 */
@Data
@Builder
public class MetricsSnapshot {

    /** Wall-clock time this snapshot was computed. */
    private Instant timestamp;

    /** Requests per second (rolling 10-second window average). */
    @JsonProperty("rps")
    private double rps;

    /** Average end-to-end latency in ms (rolling window). */
    @JsonProperty("avg_latency_ms")
    private double avgLatencyMs;

    /** 95th-percentile latency in ms (rolling window). */
    @JsonProperty("p95_latency_ms")
    private double p95LatencyMs;

    /**
     * Error rate as a fraction in [0, 1] (rolling window).
     * e.g. 0.05 = 5% errors.
     */
    @JsonProperty("error_rate")
    private double errorRate;

    /** Cumulative token count since the service started. */
    @JsonProperty("token_count")
    private long tokenCount;

    /**
     * Cache hit rate as a fraction in [0, 1] (rolling window).
     * e.g. 0.30 = 30% of requests were cache hits.
     */
    @JsonProperty("cache_hit_rate")
    private double cacheHitRate;

    /** Cumulative number of PII blocks detected since service start. */
    @JsonProperty("pii_blocked")
    private long piiBlocked;

    /**
     * The provider that handled the most recent request.
     * One of: "openai", "anthropic", "gemini", "bedrock", "azure", "local",
     * "cache".
     */
    @JsonProperty("active_provider")
    private String activeProvider;

    /** Cumulative cost in USD since the service started. */
    @JsonProperty("cost_usd_total")
    private double costUsdTotal;
}
