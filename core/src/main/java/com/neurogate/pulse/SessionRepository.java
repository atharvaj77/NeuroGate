package com.neurogate.pulse;

import org.springframework.web.socket.WebSocketSession;

import java.util.Collection;
import java.util.Optional;

public interface SessionRepository {
    void addSession(WebSocketSession session);

    void removeSession(String sessionId);

    Optional<WebSocketSession> getSession(String sessionId);

    Collection<WebSocketSession> getAllSessions();

    int getSessionCount();
}
