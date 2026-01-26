package com.neurogate.agentops;

import com.neurogate.agentops.model.Trace;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/v1/agentops")
@RequiredArgsConstructor
@Tag(name = "AgentOps", description = "Agent trace and monitoring")
public class AgentOpsController {

    private final TraceService traceService;

    @Operation(summary = "Get trace", description = "Retrieve a specific trace by ID")
    @ApiResponse(responseCode = "200", description = "Trace found")
    @ApiResponse(responseCode = "404", description = "Trace not found")
    @GetMapping("/traces/{traceId}")
    public ResponseEntity<Trace> getTrace(@Parameter(description = "Trace ID") @PathVariable String traceId) {
        return traceService.getTrace(traceId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get traces by session", description = "Retrieve all traces for a session")
    @ApiResponse(responseCode = "200", description = "Traces retrieved")
    @GetMapping("/sessions/{sessionId}/traces")
    public ResponseEntity<List<Trace>> getTracesBySession(@Parameter(description = "Session ID") @PathVariable String sessionId) {
        List<Trace> traces = traceService.getTracesBySession(sessionId);
        return ResponseEntity.ok(traces);
    }

    @Operation(summary = "Get recent traces", description = "Retrieve recent traces across all sessions")
    @ApiResponse(responseCode = "200", description = "Traces retrieved")
    @GetMapping("/traces")
    public ResponseEntity<List<Trace>> getRecentTraces(
            @Parameter(description = "Maximum number of traces") @RequestParam(defaultValue = "50") int limit) {
        List<Trace> traces = traceService.getRecentTraces(limit);
        return ResponseEntity.ok(traces);
    }

    @Operation(summary = "Get traces by user", description = "Retrieve all traces for a user")
    @ApiResponse(responseCode = "200", description = "Traces retrieved")
    @GetMapping("/users/{userId}/traces")
    public ResponseEntity<List<Trace>> getTracesByUser(@Parameter(description = "User ID") @PathVariable String userId) {
        List<Trace> traces = traceService.getTracesByUser(userId);
        return ResponseEntity.ok(traces);
    }

    @Operation(summary = "Get statistics", description = "Retrieve aggregate agent operation statistics")
    @ApiResponse(responseCode = "200", description = "Statistics retrieved")
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(traceService.getStatistics());
    }
}
