package com.neurogate.pulse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neurogate.pulse.model.PulseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * PulseEventPublisher - Broadcasts gateway events to all connected Pulse
 * Dashboard clients
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PulseEventPublisher {

    private final ObjectMapper objectMapper;
    private final SessionRepository sessionRepository;

    public void registerSession(WebSocketSession session) {
        sessionRepository.addSession(session);
        log.info("Pulse session registered: {} (total: {})", session.getId(), sessionRepository.getSessionCount());
    }

    public void unregisterSession(WebSocketSession session) {
        sessionRepository.removeSession(session.getId());
        log.info("Pulse session unregistered: {} (remaining: {})", session.getId(),
                sessionRepository.getSessionCount());
    }

    public void publish(PulseEvent event) {
        if (sessionRepository.getSessionCount() == 0) {
            return;
        }

        try {
            String eventJson = objectMapper.writeValueAsString(event);
            TextMessage message = new TextMessage(eventJson);

            sessionRepository.getAllSessions().forEach(session -> {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(message);
                    } catch (Exception e) {
                        log.error("Failed to send pulse event to session {}", session.getId(), e);
                    }
                }
            });

            log.debug("Published pulse event {} to {} clients", event.getType(), sessionRepository.getSessionCount());
        } catch (Exception e) {
            log.error("Failed to serialize pulse event", e);
        }
    }

    public int getConnectedClientCount() {
        return (int) sessionRepository.getAllSessions().stream().filter(WebSocketSession::isOpen).count();
    }
}
