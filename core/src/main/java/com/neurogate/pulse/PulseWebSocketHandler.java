package com.neurogate.pulse;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;

/**
 * PulseWebSocketHandler - Handles WebSocket connections for the Pulse Dashboard
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PulseWebSocketHandler extends TextWebSocketHandler {

    private final PulseEventPublisher pulseEventPublisher;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        pulseEventPublisher.registerSession(session);

        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                Map.of(
                        "type", "pulse_connected",
                        "session_id", session.getId(),
                        "message", "Connected to NeuroGate Pulse Dashboard",
                        "connected_clients", pulseEventPublisher.getConnectedClientCount()))));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Pulse dashboard is read-only for now, but could support commands like:
        // - Filter events by provider
        // - Pause/resume stream
        // - Request historical data
        log.debug("Received pulse command: {}", message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        pulseEventPublisher.unregisterSession(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("Pulse WebSocket transport error: {}", session.getId(), exception);
        pulseEventPublisher.unregisterSession(session);
    }
}
