package com.neurogate.sentinel.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neurogate.sentinel.SentinelService;
import com.neurogate.sentinel.model.ChatRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * WebSocket Handler for Bidirectional Streaming.
 * Provides real-time, bidirectional communication for LLM chat.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final SentinelService sentinelService;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket connection established: {}", session.getId());

        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                java.util.Map.of(
                        "type", "connection_established",
                        "session_id", session.getId(),
                        "message", "NeuroGate WebSocket connected"))));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.info("Received WebSocket message: {}", message.getPayload());

        try {
            ChatRequest request = objectMapper.readValue(message.getPayload(), ChatRequest.class);

            sentinelService.processStreamRequest(request)
                    .subscribe(
                            chunk -> {
                                try {
                                    // Send chunk to client
                                    String chunkJson = objectMapper.writeValueAsString(chunk);
                                    session.sendMessage(new TextMessage(chunkJson));

                                } catch (Exception e) {
                                    log.error("Error sending WebSocket chunk", e);
                                }
                            },
                            error -> {
                                try {
                                    log.error("Error in WebSocket stream", error);
                                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                                            java.util.Map.of(
                                                    "type", "error",
                                                    "message", error.getMessage()))));
                                } catch (Exception e) {
                                    log.error("Error sending error message", e);
                                }
                            },
                            () -> {
                                try {
                                    // Send completion message
                                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                                            java.util.Map.of(
                                                    "type", "stream_complete",
                                                    "message", "Stream completed successfully"))));

                                    log.debug("WebSocket stream completed for session: {}", session.getId());

                                } catch (Exception e) {
                                    log.error("Error completing WebSocket stream", e);
                                }
                            });

        } catch (Exception e) {
            log.error("Error processing WebSocket message", e);
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                    java.util.Map.of(
                            "type", "error",
                            "message", "Failed to process request: " + e.getMessage()))));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("WebSocket connection closed: {} - Status: {}", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error: {}", session.getId(), exception);
    }
}
