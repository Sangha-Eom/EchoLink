package com.EchoLink.auth_server.handler;

import org.json.JSONObject;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SignalingHandler extends TextWebSocketHandler {

	// 각 사용자의 이메일(key)과 WebSocket 세션(value)을 저장하는 맵
	// 동시성 문제를 피하기 위해 ConcurrentHashMap 사용
	private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

	/**
	 * Spring Security의 인증 정보에서 사용자 이메일을 가져옴.
	 * WebSocket 연결이 성공적으로 수립되었을 때 호출.
	 */
	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {

		Authentication authentication = (Authentication) session.getPrincipal();

		if (authentication != null && authentication.isAuthenticated()) {
			String userEmail = authentication.getName();
			sessions.put(userEmail, session);
			System.out.println("[Signaling] User connected: " + userEmail + " (Session ID: " + session.getId() + ")");
		} 
		else {
			// 인증되지 않은 사용자의 연결은 거부하고 세션을 닫습니다.
			System.out.println("[Signaling] Unauthorized connection attempt. Closing session.");
			session.close(CloseStatus.POLICY_VIOLATION);
		}

	}
	
	/**
	 * 클라이언트로부터 텍스트 메시지를 받았을 때 호출
	 */
	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		
		try {
			
			JSONObject jsonMessage = new JSONObject(message.getPayload());
			
			// 메시지 내용에서 'to' 필드를 찾아 수신자 이메일을 확인합니다.
			String toUserEmail = jsonMessage.optString("to");

			if (toUserEmail.isEmpty()) 
				return;

			// 수신자 이메일로 WebSocket 세션을 찾습니다.
			WebSocketSession destSession = sessions.get(toUserEmail);

			// 수신자가 존재하고, 세션이 열려있다면 메시지를 그대로 전달합니다.
			if (destSession != null && destSession.isOpen()) {
				destSession.sendMessage(new TextMessage(jsonMessage.toString()));
			}
		} 
		catch (Exception e) {
			System.err.println("[Signaling] Error handling message: " + e.getMessage());
		}
	}

	/**
	 * WebSocket 연결이 닫혔을 때 호출.
	 */
	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {

		Authentication authentication = (Authentication) session.getPrincipal();
		if (authentication != null) {
			String userEmail = authentication.getName();
			sessions.remove(userEmail);
			System.out.println("[Signaling] User disconnected: " + userEmail);
		}
	}

	/**
	 * 전송 중 에러가 발생했을 때 호출.
	 */
	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {

		System.err.println("[Signaling] Transport error: " + exception.getMessage());
		session.close(CloseStatus.SERVER_ERROR);
	}
}
