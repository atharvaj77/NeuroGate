package com.neurogate.agentops.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Trace - Represents a complete agent execution flow
 * 
 * A Trace groups multiple Spans together to represent
 * a multi-step agent workflow (e.g., ReAct agent chain).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "traces")
public class Trace {

    @Id
    @JsonProperty("trace_id")
    private String traceId;

    @JsonProperty("session_id")
    private String sessionId;

    private String name;

    @JsonProperty("start_time")
    private Instant startTime;

    @JsonProperty("end_time")
    private Instant endTime;

    @JsonProperty("duration_ms")
    private Long durationMs;

    @Enumerated(EnumType.STRING)
    private TraceStatus status;

    @Builder.Default
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "trace_id")
    private List<Span> spans = new ArrayList<>();

    @Transient
    private Map<String, Object> metadata;

    @JsonProperty("total_tokens")
    private Integer totalTokens;

    @JsonProperty("total_cost_usd")
    private Double totalCostUsd;

    @JsonProperty("user_id")
    private String userId;

    @Transient // Skipped for simplicity in this migration
    @JsonProperty("user_feedback")
    private UserFeedback userFeedback;

    public enum TraceStatus {
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    public String getError() {
        if (spans == null)
            return null;
        return spans.stream()
                .filter(s -> s.getError() != null)
                .map(Span::getError)
                .findFirst()
                .orElse(null);
    }

    public String getInput() {
        if (spans == null || spans.isEmpty())
            return null;
        return spans.get(0).getInput();
    }

    public String getOutput() {
        if (spans == null || spans.isEmpty())
            return null;
        return spans.get(spans.size() - 1).getOutput();
    }

    /**
     * Add a span to this trace
     */
    public void addSpan(Span span) {
        if (spans == null) {
            spans = new ArrayList<>();
        }
        spans.add(span);
    }

    /**
     * Calculate total tokens and cost from all spans
     */
    public void calculateTotals() {
        if (spans != null) {
            totalTokens = spans.stream()
                    .filter(s -> s.getTokenCount() != null)
                    .mapToInt(Span::getTokenCount)
                    .sum();
            totalCostUsd = spans.stream()
                    .filter(s -> s.getCostUsd() != null)
                    .mapToDouble(Span::getCostUsd)
                    .sum();
        }
    }
}
