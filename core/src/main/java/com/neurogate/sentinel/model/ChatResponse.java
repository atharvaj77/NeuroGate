package com.neurogate.sentinel.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * OpenAI-compatible Chat Completion Response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    private String id;

    private String object;

    private String traceId;
    private String sessionId;

    private Long created;

    private String model;

    private List<Choice> choices;

    private Usage usage;

    @JsonProperty("system_fingerprint")
    private String systemFingerprint;

    // NeuroGate-specific metadata
    @JsonProperty("x_neurogate_cache_hit")
    private Boolean cacheHit;

    @JsonProperty("x_neurogate_route")
    private String route;

    @JsonProperty("x_neurogate_latency_ms")
    private Long latencyMs;

    @JsonProperty("x_neurogate_pii_detected")
    private Integer piiDetected;

    @JsonProperty("x_neurogate_error")
    private String error;

    private Double costUsd;

    @JsonProperty("x_neurogate_similarity")
    private Double similarity;

    @JsonProperty("x_neurogate_citations")
    private List<String> citations;

    public boolean isCacheHit() {
        return Boolean.TRUE.equals(cacheHit);
    }
}
