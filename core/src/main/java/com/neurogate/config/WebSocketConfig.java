package com.neurogate.config;

import com.neurogate.pulse.PulseWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket Configuration.
 * Enables bidirectional streaming for real-time LLM responses.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final com.neurogate.sentinel.websocket.ChatWebSocketHandler chatWebSocketHandler;
    private final PulseWebSocketHandler pulseWebSocketHandler;

    public WebSocketConfig(
            com.neurogate.sentinel.websocket.ChatWebSocketHandler chatWebSocketHandler,
            PulseWebSocketHandler pulseWebSocketHandler) {
        this.chatWebSocketHandler = chatWebSocketHandler;
        this.pulseWebSocketHandler = pulseWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .setAllowedOrigins("*");

        registry.addHandler(pulseWebSocketHandler, "/ws/pulse")
                .setAllowedOrigins("*");
    }
}
