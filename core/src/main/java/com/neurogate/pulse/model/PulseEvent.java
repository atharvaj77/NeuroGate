package com.neurogate.pulse.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * PulseEvent - Real-time event for the Pulse Dashboard
 * 
 * Represents a single gateway event (request, response, error)
 * that is streamed to connected dashboard clients.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PulseEvent {

    public enum EventType {
        REQUEST_RECEIVED,
        REQUEST_ROUTED,
        RESPONSE_SENT,
        CACHE_HIT,
        PROVIDER_FALLBACK,
        CIRCUIT_OPENED,

        ERROR,
        METRIC_UPDATE
    }

    private String id;

    private EventType type;

    private Instant timestamp;

    private String provider;

    private String model;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("latency_ms")
    private Long latencyMs;

    @JsonProperty("token_count")
    private Integer tokenCount;

    @JsonProperty("cost_usd")
    private Double costUsd;

    @JsonProperty("cache_hit")
    private Boolean cacheHit;

    private String error;

    private String message;

    @JsonProperty("payload")
    private Object payload;
}
