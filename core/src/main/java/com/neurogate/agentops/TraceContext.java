package com.neurogate.agentops;

import com.neurogate.agentops.model.Span;
import com.neurogate.agentops.model.Trace;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.UUID;

/**
 * TraceContext - Thread-local context for trace propagation
 * 
 * Maintains the current trace and span for the executing thread,
 * enabling automatic context propagation across nested calls.
 */
@Slf4j
public class TraceContext {

    private static final ThreadLocal<Trace> currentTrace = new ThreadLocal<>();
    private static final ThreadLocal<Span> currentSpan = new ThreadLocal<>();

    public static Trace startTrace(String name, String sessionId, String userId) {
        Trace trace = Trace.builder()
                .traceId(UUID.randomUUID().toString())
                .sessionId(sessionId != null ? sessionId : UUID.randomUUID().toString())
                .name(name)
                .startTime(Instant.now())
                .status(Trace.TraceStatus.RUNNING)
                .userId(userId)
                .build();

        currentTrace.set(trace);
        log.debug("Started trace: {} - {}", trace.getTraceId(), name);
        return trace;
    }

    public static Span startSpan(String name, Span.SpanType type) {
        Trace trace = currentTrace.get();
        if (trace == null) {
            log.warn("No active trace, creating orphan span");
            trace = startTrace("orphan-trace", null, null);
        }

        Span parentSpan = currentSpan.get();

        Span span = Span.builder()
                .spanId(UUID.randomUUID().toString())
                .traceId(trace.getTraceId())
                .parentSpanId(parentSpan != null ? parentSpan.getSpanId() : null)
                .name(name)
                .type(type)
                .startTime(Instant.now())
                .status(Span.SpanStatus.RUNNING)
                .build();

        trace.addSpan(span);
        currentSpan.set(span);

        log.debug("Started span: {} - {} (parent: {})", span.getSpanId(), name,
                parentSpan != null ? parentSpan.getSpanId() : "none");
        return span;
    }

    public static void endSpan() {
        Span span = currentSpan.get();
        if (span != null) {
            span.complete();
            log.debug("Ended span: {} - {}ms", span.getSpanId(), span.getDurationMs());
            currentSpan.remove();
        }
    }

    public static Trace endTrace() {
        Trace trace = currentTrace.get();
        if (trace != null) {
            trace.setEndTime(Instant.now());
            trace.setDurationMs(trace.getEndTime().toEpochMilli() - trace.getStartTime().toEpochMilli());
            trace.setStatus(Trace.TraceStatus.COMPLETED);
            trace.calculateTotals();
            log.debug("Ended trace: {} - {}ms", trace.getTraceId(), trace.getDurationMs());
            currentTrace.remove();
        }
        return trace;
    }

    public static Trace getCurrentTrace() {
        return currentTrace.get();
    }

    public static Span getCurrentSpan() {
        return currentSpan.get();
    }

    public static String getTraceId() {
        Trace trace = currentTrace.get();
        return trace != null ? trace.getTraceId() : null;
    }

    public static void clear() {
        currentTrace.remove();
        currentSpan.remove();
    }
}
