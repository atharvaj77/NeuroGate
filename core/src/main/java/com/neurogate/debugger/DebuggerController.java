package com.neurogate.debugger;

import com.neurogate.sentinel.model.ChatResponse;
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

/**
 * REST API for AI Debugger.
 * Provides endpoints for searching records, creating sessions, and replaying
 * requests.
 */
@Slf4j
@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
@Tag(name = "Debugger", description = "Session replay and debugging")
public class DebuggerController {

    private final AIDebuggerService debuggerService;

    @Operation(summary = "Search debug records", description = "Search debug records with filters")
    @ApiResponse(responseCode = "200", description = "Records retrieved")
    @GetMapping("/records")
    public ResponseEntity<List<DebugRecord>> searchRecords(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) Double minCost,
            @RequestParam(required = false) Long maxLatency,
            @RequestParam(required = false, defaultValue = "100") Integer limit) {

        DebugSearchFilter filter = DebugSearchFilter.builder()
                .userId(userId)
                .provider(provider)
                .minCost(minCost)
                .maxLatency(maxLatency)
                .limit(limit)
                .build();

        List<DebugRecord> records = debuggerService.searchRecords(filter);
        return ResponseEntity.ok(records);
    }

    @Operation(summary = "Get user debug history", description = "Retrieve debug records for a specific user")
    @ApiResponse(responseCode = "200", description = "Records retrieved")
    @GetMapping("/users/{userId}/records")
    public ResponseEntity<List<DebugRecord>> getUserRecords(
            @PathVariable String userId,
            @RequestParam(required = false, defaultValue = "100") Integer limit) {

        List<DebugRecord> records = debuggerService.getUserRecords(userId, limit);
        return ResponseEntity.ok(records);
    }

    @Operation(summary = "Create debug session", description = "Create a new debug session for a request")
    @ApiResponse(responseCode = "200", description = "Session created")
    @PostMapping("/sessions")
    public ResponseEntity<DebugSession> createSession(
            @RequestParam String requestId) {

        DebugSession session = debuggerService.createSession(requestId);
        return ResponseEntity.ok(session);
    }

    @Operation(summary = "Replay request", description = "Replay a request with modifications")
    @ApiResponse(responseCode = "200", description = "Replay completed")
    @PostMapping("/sessions/{sessionId}/replay")
    public ResponseEntity<ChatResponse> replayRequest(
            @PathVariable String sessionId,
            @RequestBody ReplayOptions options) {

        log.info("Replaying request: sessionId={}, model={}", sessionId, options.getModel());

        ChatResponse response = debuggerService.replay(sessionId, options);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Fork session", description = "Fork a session from a specific step")
    @ApiResponse(responseCode = "200", description = "Session forked")
    @PostMapping("/sessions/{sessionId}/fork")
    public ResponseEntity<DebugSession> forkSession(
            @PathVariable String sessionId,
            @RequestParam String stepId,
            @RequestBody(required = false) Map<String, Object> modifications) {

        DebugSession newSession = debuggerService.forkSession(sessionId, stepId, modifications);
        return ResponseEntity.ok(newSession);
    }

    @Operation(summary = "Get semantic diff", description = "Compare original and replay responses")
    @ApiResponse(responseCode = "200", description = "Diff computed")
    @GetMapping("/sessions/{sessionId}/diff")
    public ResponseEntity<SemanticDiff> getSemanticDiff(
            @PathVariable String sessionId) {

        SemanticDiff diff = debuggerService.compareResponses(sessionId);
        return ResponseEntity.ok(diff);
    }

    @Operation(summary = "Export session", description = "Export debug session as JSON")
    @ApiResponse(responseCode = "200", description = "Session exported")
    @GetMapping("/sessions/{sessionId}/export")
    public ResponseEntity<String> exportSession(
            @PathVariable String sessionId) {

        String exportJson = debuggerService.exportSession(sessionId);
        return ResponseEntity.ok(exportJson);
    }

    @Operation(summary = "Cleanup records", description = "Remove debug records older than retention period")
    @ApiResponse(responseCode = "200", description = "Cleanup completed")
    @DeleteMapping("/records/cleanup")
    public ResponseEntity<String> cleanupRecords(
            @RequestParam(defaultValue = "30") int retentionDays) {

        debuggerService.cleanupOldRecords(retentionDays);
        return ResponseEntity.ok(String.format("Cleaned up records older than %d days", retentionDays));
    }
}
