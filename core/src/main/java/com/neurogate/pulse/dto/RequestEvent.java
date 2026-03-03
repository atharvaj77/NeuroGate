package com.neurogate.pulse.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * RequestEvent — a single gateway request event broadcast via WebSocket to the
 * Pulse Dashboard feed. Used for the per-request live log / waterfall preview.
 */
@Data
@Builder
public class RequestEvent {

    /** Unique request identifier (UUID). */
    @JsonProperty("request_id")
    private String requestId;

    /** ISO timestamp when the request was received by the gateway. */
    private Instant timestamp;

    /** LLM model used (e.g. "gpt-4o", "claude-3-sonnet"). */
    private String model;

    /** Upstream provider (e.g. "openai", "anthropic", "gemini"). */
    private String provider;

    /** End-to-end latency in milliseconds. */
    @JsonProperty("latency_ms")
    private long latencyMs;

    /**
     * HTTP-style status string.
     * One of: "SUCCESS", "ERROR", "CACHE_HIT", "RATE_LIMITED".
     */
    private String status;

    /** Whether this request was served from cache (any tier). */
    @JsonProperty("cache_hit")
    private boolean cacheHit;

    /** Whether PII was detected (and masked) in the request. */
    @JsonProperty("pii_detected")
    private boolean piiDetected;

    /** Estimated cost of this specific request in USD. */
    @JsonProperty("cost_usd")
    private double costUsd;

    /** Total tokens consumed (prompt + completion). */
    @JsonProperty("token_count")
    private int tokenCount;
}
