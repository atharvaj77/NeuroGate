package com.neurogate.agentops;

import com.neurogate.agentops.model.Trace;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * TraceFilter - Automatically creates/propagates traces for incoming requests
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class TraceFilter extends OncePerRequestFilter {

    private final TraceService traceService;

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String SESSION_ID_HEADER = "X-Session-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        if (!path.startsWith("/v1/")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String sessionId = request.getHeader(SESSION_ID_HEADER);
            String userId = request.getHeader("X-User-Id");
            Trace trace = TraceContext.startTrace(
                    request.getMethod() + " " + path,
                    sessionId,
                    userId);

            MDC.put("traceId", trace.getTraceId());
            MDC.put("sessionId", trace.getSessionId());
            response.setHeader(TRACE_ID_HEADER, trace.getTraceId());
            response.setHeader(SESSION_ID_HEADER, trace.getSessionId());

            try {
                filterChain.doFilter(request, response);
            } finally {
                Trace completedTrace = TraceContext.endTrace();
                if (completedTrace != null) {
                    traceService.saveTrace(completedTrace);
                }
            }

        } finally {
            TraceContext.clear();
            MDC.clear();
        }
    }
}
