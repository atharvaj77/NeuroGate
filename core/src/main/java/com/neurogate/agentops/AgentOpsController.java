package com.neurogate.agentops;

import com.neurogate.agentops.model.Trace;
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
public class AgentOpsController {

    private final TraceService traceService;

    @GetMapping("/traces/{traceId}")
    public ResponseEntity<Trace> getTrace(@PathVariable String traceId) {
        return traceService.getTrace(traceId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/sessions/{sessionId}/traces")
    public ResponseEntity<List<Trace>> getTracesBySession(@PathVariable String sessionId) {
        List<Trace> traces = traceService.getTracesBySession(sessionId);
        return ResponseEntity.ok(traces);
    }

    @GetMapping("/traces")
    public ResponseEntity<List<Trace>> getRecentTraces(
            @RequestParam(defaultValue = "50") int limit) {
        List<Trace> traces = traceService.getRecentTraces(limit);
        return ResponseEntity.ok(traces);
    }

    @GetMapping("/users/{userId}/traces")
    public ResponseEntity<List<Trace>> getTracesByUser(@PathVariable String userId) {
        List<Trace> traces = traceService.getTracesByUser(userId);
        return ResponseEntity.ok(traces);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(traceService.getStatistics());
    }
}
