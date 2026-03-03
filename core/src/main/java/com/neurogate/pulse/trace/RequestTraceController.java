package com.neurogate.pulse.trace;

import com.neurogate.agentops.TraceService;
import com.neurogate.agentops.model.Span;
import com.neurogate.agentops.model.Trace;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * RequestTraceController — exposes the waterfall trace view for a single
 * NeuroGate request.
 *
 * <p>
 * Delegates to the existing {@link TraceService} / AgentOps store and
 * re-maps {@link Span} objects into a human-readable waterfall format suitable
 * for the Pulse Dashboard front-end.
 *
 * <p>
 * Endpoint:
 * 
 * <pre>
 * GET /api/v1/traces/{requestId}
 * </pre>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/traces")
@RequiredArgsConstructor
@Tag(name = "Request Traces", description = "Per-request waterfall trace view")
public class RequestTraceController {

    private final TraceService traceService;

    // -----------------------------------------------------------------------
    // Waterfall trace endpoint
    // -----------------------------------------------------------------------

    @Operation(summary = "Get request trace waterfall", description = """
            Returns the full execution waterfall for a single request,
            with per-stage timings (Auth, PII Scan, Cache Lookup, LLM Call,
            PII Restore, Response).
            Timings are reconstructed from the AgentOps Span records.
            """)
    @GetMapping("/{requestId}")
    public ResponseEntity<TraceWaterfall> getTrace(
            @Parameter(description = "Request / Trace ID (UUID)") @PathVariable String requestId) {

        Optional<Trace> traceOpt = traceService.getTrace(requestId);
        if (traceOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Trace trace = traceOpt.get();
        TraceWaterfall waterfall = buildWaterfall(trace);
        return ResponseEntity.ok(waterfall);
    }

    // -----------------------------------------------------------------------
    // Waterfall construction
    // -----------------------------------------------------------------------

    private TraceWaterfall buildWaterfall(Trace trace) {
        List<WaterfallStage> stages = new ArrayList<>();

        if (trace.getSpans() != null) {
            for (Span span : trace.getSpans()) {
                long durationMs = 0;
                if (span.getStartTime() != null && span.getEndTime() != null) {
                    durationMs = span.getEndTime().toEpochMilli() - span.getStartTime().toEpochMilli();
                }

                StageColor color = classifyColor(durationMs);

                stages.add(WaterfallStage.builder()
                        .name(humanReadableStageName(span.getName()))
                        .durationMs(durationMs)
                        .startTime(span.getStartTime())
                        .endTime(span.getEndTime())
                        .color(color.name().toLowerCase())
                        .detail(buildDetail(span))
                        .error(span.getError())
                        .build());
            }
        }

        long totalMs = 0;
        if (trace.getStartTime() != null && trace.getEndTime() != null) {
            totalMs = trace.getEndTime().toEpochMilli() - trace.getStartTime().toEpochMilli();
        }

        return TraceWaterfall.builder()
                .requestId(trace.getTraceId())
                .startTime(trace.getStartTime())
                .endTime(trace.getEndTime())
                .totalMs(totalMs)
                .status(trace.getStatus() != null ? trace.getStatus().name() : "UNKNOWN")
                .stages(stages)
                .totalTokens(trace.getTotalTokens())
                .totalCostUsd(trace.getTotalCostUsd())
                .userId(trace.getUserId())
                .build();
    }

    /**
     * Maps internal span names to human-readable stage labels shown in the UI.
     */
    private String humanReadableStageName(String spanName) {
        if (spanName == null)
            return "Unknown";
        return switch (spanName.toLowerCase()) {
            case "auth", "authenticate" -> "Auth";
            case "pii_scan", "pii-scan" -> "PII Scan";
            case "cache", "cache_lookup" -> "Cache Lookup";
            case "llm", "llm_call", "upstream" -> "LLM Call";
            case "pii_restore" -> "PII Restore";
            case "response", "send_response" -> "Response";
            default -> spanName;
        };
    }

    /**
     * Color-codes stages by duration for the UI heatmap.
     */
    private StageColor classifyColor(long durationMs) {
        if (durationMs < 50)
            return StageColor.GREEN;
        if (durationMs < 200)
            return StageColor.YELLOW;
        return StageColor.RED;
    }

    private String buildDetail(Span span) {
        StringBuilder sb = new StringBuilder();
        if (span.getInput() != null)
            sb.append("input_len=").append(span.getInput().length()).append(" ");
        if (span.getTokenCount() != null)
            sb.append("tokens=").append(span.getTokenCount()).append(" ");
        if (span.getCostUsd() != null)
            sb.append("cost=$").append(String.format("%.6f", span.getCostUsd()));
        return sb.toString().trim();
    }

    // -----------------------------------------------------------------------
    // Response DTOs
    // -----------------------------------------------------------------------

    @Data
    @Builder
    public static class TraceWaterfall {
        private String requestId;
        private Instant startTime;
        private Instant endTime;
        private long totalMs;
        private String status;
        private List<WaterfallStage> stages;
        private Integer totalTokens;
        private Double totalCostUsd;
        private String userId;
    }

    @Data
    @Builder
    public static class WaterfallStage {
        private String name;
        private long durationMs;
        private Instant startTime;
        private Instant endTime;
        /** "green" | "yellow" | "red" */
        private String color;
        private String detail;
        private String error;
    }

    private enum StageColor {
        GREEN, YELLOW, RED
    }
}
