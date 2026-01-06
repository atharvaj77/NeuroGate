package com.neurogate.agentops.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Span - Represents a single LLM call or tool invocation within a Trace
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "spans")
public class Span {

    @Id
    @JsonProperty("span_id")
    private String spanId;

    @JsonProperty("trace_id")
    private String traceId;

    @JsonProperty("parent_span_id")
    private String parentSpanId;

    private String name;

    @Enumerated(EnumType.STRING)
    private SpanType type;

    @JsonProperty("start_time")
    private Instant startTime;

    @JsonProperty("end_time")
    private Instant endTime;

    @JsonProperty("duration_ms")
    private Long durationMs;

    @Enumerated(EnumType.STRING)
    private SpanStatus status;

    // LLM-specific fields
    private String provider;
    private String model;

    @Column(length = 5000)
    private String input;

    @Column(length = 5000)
    private String output;

    @JsonProperty("token_count")
    private Integer tokenCount;

    @JsonProperty("cost_usd")
    private Double costUsd;

    // Tool-specific fields
    @JsonProperty("tool_name")
    private String toolName;

    @Transient
    @JsonProperty("tool_input")
    private Map<String, Object> toolInput;

    @Transient
    @JsonProperty("tool_output")
    private Object toolOutput;

    private String error;

    @Transient
    private Map<String, Object> metadata;

    public enum SpanType {
        LLM_CALL,
        TOOL_CALL,
        RETRIEVAL,
        REASONING,
        CUSTOM
    }

    public enum SpanStatus {
        RUNNING,
        COMPLETED,
        FAILED
    }

    /**
     * Complete this span
     */
    public void complete() {
        this.endTime = Instant.now();
        this.durationMs = endTime.toEpochMilli() - startTime.toEpochMilli();
        this.status = SpanStatus.COMPLETED;
    }

    /**
     * Mark this span as failed
     */
    public void fail(String errorMessage) {
        this.endTime = Instant.now();
        this.durationMs = endTime.toEpochMilli() - startTime.toEpochMilli();
        this.status = SpanStatus.FAILED;
        this.error = errorMessage;
    }
}
