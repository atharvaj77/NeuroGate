package com.neurogate.ops.observability;

import com.neurogate.agentops.model.Span;
import com.neurogate.agentops.model.Trace;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Bridge between NeuroGate's AgentOps tracing and OpenTelemetry.
 *
 * Converts NeuroGate Trace/Span objects to OTEL spans for export to
 * distributed tracing backends (Jaeger, Zipkin, Grafana Tempo, etc.).
 *
 * Semantic conventions for AI/LLM observability:
 * - gen_ai.system: The AI system (openai, anthropic, etc.)
 * - gen_ai.request.model: Model name
 * - gen_ai.usage.input_tokens: Input token count
 * - gen_ai.usage.output_tokens: Output token count
 * - gen_ai.response.finish_reason: Completion reason
 */
@Slf4j
@Component
@ConditionalOnBean(Tracer.class)
@RequiredArgsConstructor
public class OtelTraceBridge {

    private final Tracer tracer;

    // Cache of active OTEL spans keyed by NeuroGate span ID
    private final Map<String, io.opentelemetry.api.trace.Span> activeSpans = new ConcurrentHashMap<>();

    // Semantic convention attribute keys for LLM observability
    private static final AttributeKey<String> GEN_AI_SYSTEM = AttributeKey.stringKey("gen_ai.system");
    private static final AttributeKey<String> GEN_AI_REQUEST_MODEL = AttributeKey.stringKey("gen_ai.request.model");
    private static final AttributeKey<Long> GEN_AI_USAGE_INPUT_TOKENS = AttributeKey.longKey("gen_ai.usage.input_tokens");
    private static final AttributeKey<Long> GEN_AI_USAGE_OUTPUT_TOKENS = AttributeKey.longKey("gen_ai.usage.output_tokens");
    private static final AttributeKey<Double> GEN_AI_USAGE_COST = AttributeKey.doubleKey("gen_ai.usage.cost_usd");
    private static final AttributeKey<String> NEUROGATE_TRACE_ID = AttributeKey.stringKey("neurogate.trace_id");
    private static final AttributeKey<String> NEUROGATE_SESSION_ID = AttributeKey.stringKey("neurogate.session_id");
    private static final AttributeKey<String> NEUROGATE_USER_ID = AttributeKey.stringKey("neurogate.user_id");
    private static final AttributeKey<String> NEUROGATE_SPAN_TYPE = AttributeKey.stringKey("neurogate.span_type");
    private static final AttributeKey<String> TOOL_NAME = AttributeKey.stringKey("tool.name");

    /**
     * Export a complete NeuroGate trace to OpenTelemetry.
     * Call this after a trace is complete.
     */
    public void exportTrace(Trace trace) {
        if (trace == null || trace.getSpans() == null || trace.getSpans().isEmpty()) {
            log.debug("Skipping empty trace");
            return;
        }

        log.debug("Exporting trace {} with {} spans to OTEL", trace.getTraceId(), trace.getSpans().size());

        try {
            // Create the root span for the trace
            Instant startTime = trace.getStartTime() != null ? trace.getStartTime() : Instant.now();
            io.opentelemetry.api.trace.Span rootSpan = tracer.spanBuilder(trace.getName() != null ? trace.getName() : "neurogate-trace")
                    .setSpanKind(SpanKind.SERVER)
                    .setStartTimestamp(toEpochNanos(startTime), TimeUnit.NANOSECONDS)
                    .setAttribute(NEUROGATE_TRACE_ID, trace.getTraceId())
                    .setAttribute(NEUROGATE_SESSION_ID, trace.getSessionId() != null ? trace.getSessionId() : "")
                    .setAttribute(NEUROGATE_USER_ID, trace.getUserId() != null ? trace.getUserId() : "")
                    .startSpan();

            if (trace.getTotalTokens() != null) {
                rootSpan.setAttribute(GEN_AI_USAGE_INPUT_TOKENS, trace.getTotalTokens().longValue());
            }
            if (trace.getTotalCostUsd() != null) {
                rootSpan.setAttribute(GEN_AI_USAGE_COST, trace.getTotalCostUsd());
            }

            Context rootContext = Context.current().with(rootSpan);

            // Export each span as a child
            for (Span ngSpan : trace.getSpans()) {
                exportSpan(ngSpan, rootContext);
            }

            // Set status and end root span
            if (trace.getStatus() == Trace.TraceStatus.FAILED) {
                rootSpan.setStatus(StatusCode.ERROR, trace.getError());
            } else {
                rootSpan.setStatus(StatusCode.OK);
            }

            Instant endTime = trace.getEndTime() != null ? trace.getEndTime() : Instant.now();
            rootSpan.end(toEpochNanos(endTime), TimeUnit.NANOSECONDS);

        } catch (Exception e) {
            log.error("Failed to export trace {} to OTEL", trace.getTraceId(), e);
        }
    }

    /**
     * Export a single NeuroGate span to OpenTelemetry.
     */
    public void exportSpan(Span ngSpan, Context parentContext) {
        if (ngSpan == null) return;

        try {
            Instant spanStartTime = ngSpan.getStartTime() != null ? ngSpan.getStartTime() : Instant.now();
            SpanBuilder spanBuilder = tracer.spanBuilder(ngSpan.getName() != null ? ngSpan.getName() : "span")
                    .setParent(parentContext)
                    .setSpanKind(mapSpanKind(ngSpan.getType()))
                    .setStartTimestamp(toEpochNanos(spanStartTime), TimeUnit.NANOSECONDS);

            // Add span type
            if (ngSpan.getType() != null) {
                spanBuilder.setAttribute(NEUROGATE_SPAN_TYPE, ngSpan.getType().name());
            }

            // Add LLM-specific attributes
            if (ngSpan.getProvider() != null) {
                spanBuilder.setAttribute(GEN_AI_SYSTEM, ngSpan.getProvider());
            }
            if (ngSpan.getModel() != null) {
                spanBuilder.setAttribute(GEN_AI_REQUEST_MODEL, ngSpan.getModel());
            }
            if (ngSpan.getTokenCount() != null) {
                spanBuilder.setAttribute(GEN_AI_USAGE_OUTPUT_TOKENS, ngSpan.getTokenCount().longValue());
            }
            if (ngSpan.getCostUsd() != null) {
                spanBuilder.setAttribute(GEN_AI_USAGE_COST, ngSpan.getCostUsd());
            }

            // Add tool-specific attributes
            if (ngSpan.getToolName() != null) {
                spanBuilder.setAttribute(TOOL_NAME, ngSpan.getToolName());
            }

            io.opentelemetry.api.trace.Span otelSpan = spanBuilder.startSpan();

            // Add input/output as events (to avoid large attributes)
            if (ngSpan.getInput() != null && ngSpan.getInput().length() < 1000) {
                otelSpan.addEvent("input", Attributes.of(AttributeKey.stringKey("content"),
                        truncate(ngSpan.getInput(), 500)));
            }
            if (ngSpan.getOutput() != null && ngSpan.getOutput().length() < 1000) {
                otelSpan.addEvent("output", Attributes.of(AttributeKey.stringKey("content"),
                        truncate(ngSpan.getOutput(), 500)));
            }

            // Set status
            if (ngSpan.getStatus() == Span.SpanStatus.FAILED) {
                otelSpan.setStatus(StatusCode.ERROR, ngSpan.getError());
                if (ngSpan.getError() != null) {
                    otelSpan.recordException(new RuntimeException(ngSpan.getError()));
                }
            } else {
                otelSpan.setStatus(StatusCode.OK);
            }

            // End span
            Instant spanEndTime = ngSpan.getEndTime() != null ? ngSpan.getEndTime() : Instant.now();
            otelSpan.end(toEpochNanos(spanEndTime), TimeUnit.NANOSECONDS);

        } catch (Exception e) {
            log.error("Failed to export span {} to OTEL", ngSpan.getSpanId(), e);
        }
    }

    /**
     * Start a new OTEL span that can be ended later.
     * Use this for real-time tracing during request processing.
     */
    public io.opentelemetry.api.trace.Span startSpan(String name, String ngSpanId, Context parentContext) {
        io.opentelemetry.api.trace.Span span = tracer.spanBuilder(name)
                .setParent(parentContext != null ? parentContext : Context.current())
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();

        if (ngSpanId != null) {
            activeSpans.put(ngSpanId, span);
        }

        return span;
    }

    /**
     * End an active span by NeuroGate span ID.
     */
    public void endSpan(String ngSpanId) {
        io.opentelemetry.api.trace.Span span = activeSpans.remove(ngSpanId);
        if (span != null) {
            span.end();
        }
    }

    /**
     * Record an exception on an active span.
     */
    public void recordException(String ngSpanId, Throwable exception) {
        io.opentelemetry.api.trace.Span span = activeSpans.get(ngSpanId);
        if (span != null) {
            span.recordException(exception);
            span.setStatus(StatusCode.ERROR, exception.getMessage());
        }
    }

    /**
     * Add attributes to an active span.
     */
    public void addAttributes(String ngSpanId, Map<String, String> attributes) {
        io.opentelemetry.api.trace.Span span = activeSpans.get(ngSpanId);
        if (span != null && attributes != null) {
            attributes.forEach((k, v) -> span.setAttribute(AttributeKey.stringKey(k), v));
        }
    }

    private SpanKind mapSpanKind(Span.SpanType type) {
        if (type == null) return SpanKind.INTERNAL;
        return switch (type) {
            case LLM_CALL -> SpanKind.CLIENT;  // Outgoing call to LLM
            case TOOL_CALL -> SpanKind.CLIENT; // Outgoing call to tool
            case RETRIEVAL -> SpanKind.CLIENT; // Outgoing call to vector DB
            default -> SpanKind.INTERNAL;
        };
    }

    private String truncate(String str, int maxLength) {
        if (str == null) return "";
        return str.length() <= maxLength ? str : str.substring(0, maxLength) + "...";
    }

    /**
     * Convert Instant to epoch nanoseconds.
     */
    private long toEpochNanos(Instant instant) {
        return instant.getEpochSecond() * 1_000_000_000L + instant.getNano();
    }
}