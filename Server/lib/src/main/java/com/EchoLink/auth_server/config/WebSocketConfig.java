package com.EchoLink.auth_server.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.EchoLink.auth_server.handler.SignalingHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private SignalingHandler signalingHandler; // Signaling 핸들러 객체

    /**
     * WebSocket 
     * 
     * 클라이언트가 "ws://서버주소/signaling"으로 접속하면 signalingHandler가 동작하도록 설정
     * setAllowedOrigins("*")는 모든 도메인에서의 접속을 허용(CORS 설정).
     * @param registry
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(signalingHandler, "/signaling").setAllowedOrigins("*");
    }
}
