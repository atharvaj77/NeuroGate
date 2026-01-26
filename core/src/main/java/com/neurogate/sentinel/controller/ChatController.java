package com.neurogate.sentinel.controller;

import com.neurogate.sentinel.SentinelService;
import com.neurogate.sentinel.model.ChatRequest;
import com.neurogate.sentinel.model.ChatResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import jakarta.validation.Valid;

/**
 * Main entry point for chat completion requests.
 * Mimics the OpenAI API specification at /v1/chat/completions.
 */
@Slf4j
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Validated
@Tag(name = "Chat", description = "OpenAI-compatible chat completions API")
public class ChatController {

    private final SentinelService sentinelService;

    @Operation(
        summary = "Create chat completion",
        description = "Send a chat completion request. Supports streaming via SSE when stream=true."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Successful completion",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ChatResponse.class)
            )
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping(value = "/chat/completions", produces = { MediaType.APPLICATION_JSON_VALUE,
            MediaType.TEXT_EVENT_STREAM_VALUE })
    public ResponseEntity<?> createChatCompletion(
            @Valid @RequestBody ChatRequest request,
            @Parameter(description = "Canary weight for A/B testing (0-100)")
            @RequestHeader(value = "X-Canary-Weight", required = false) Integer canaryWeight,
            @Parameter(description = "Trace ID for distributed tracing")
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @Parameter(description = "Session ID for conversation tracking")
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {

        if (canaryWeight != null) {
            request.setCanaryWeight(canaryWeight);
        }

        if (traceId != null) {
            request.setTraceId(traceId);
        }
        if (sessionId != null) {
            request.setSessionId(sessionId);
        }

        if (request.getTraceId() != null) {
            org.slf4j.MDC.put("traceId", request.getTraceId());
        }
        if (request.getSessionId() != null) {
            org.slf4j.MDC.put("sessionId", request.getSessionId());
        }

        try {
            if (Boolean.TRUE.equals(request.getStream())) {
                return ResponseEntity.ok()
                        .contentType(MediaType.TEXT_EVENT_STREAM)
                        .body(sentinelService.processStreamRequest(request));
            } else {
                ChatResponse response = sentinelService.processRequest(request);
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(response);
            }
        } finally {
            org.slf4j.MDC.clear();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("NeuroGate is running");
    }
}
