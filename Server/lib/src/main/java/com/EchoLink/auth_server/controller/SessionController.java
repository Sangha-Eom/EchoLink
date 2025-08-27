package com.EchoLink.auth_server.controller;

import com.EchoLink.auth_server.util.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 세션 생성 API 클래스 (/api/sessions)
 * 
 * 데스크탑 서버와 모바일 앱 간 연결을 위한  토큰 기반 세션 생성.
 * - 클라이언트(모바일)은 JWT 전달
 * - 인증 서버가 토큰 검증 -> 세선 ID 발급 -> 클라이언트가 이를 사용해 서로 통신
 * 
 * 전체 흐름
 * - 사용자 로그인(OAuth2) → JWT 보유
 * - 모바일 앱: POST /api/sessions (Bearer JWT) → sessionId 획득
 * - 모바일 앱 → 데스크톱 서버: TCP/WS 초기 핸드셰이크에 sessionId 전달
 * - 데스크톱 서버 → 인증 서버: GET /api/sessions/{sessionId}
 * ㄴ 200이면 이메일 확인 후, 소유자와 일치하면 스트리밍/입력 세션 허용
 * ㄴ 404면 거절
 * @author ESH
 */
@RestController
@RequestMapping("/api/sessions")
public class SessionController {

	private final JwtUtil jwtUtil;

	// 메모리 기반 세션 저장소 
	// TODO: 추후 Redis 외부 저장소로 확장
	private final Map<String, String> activeSessions = new HashMap<>();

	/**
	 * 생성자
	 * @param jwtUtil
	 */
	public SessionController(JwtUtil jwtUtil) {
		this.jwtUtil = jwtUtil;
	}

	
	/**
	 * 연결에 사용할 '임시 세션' 생성 엔드포인트 메소드
	 * 
	 * - 클라이언트의 JWT 유효성 확인
	 * - 새 세션ID(UUID) 만들어 로그인한 사용자 이메일과 매핑해 서버메모리(activeSessions)에 저장
	 * - 응답으로 sessionID + email 반환
	 * ㄴ sessionID는 PC 제어 세션을 여는 일회용 열쇠처럼 사용
	 * 
	 * @param authHeader
	 * @return 응답용 sessionID + email
	 */
	@PostMapping
	public ResponseEntity<?> createSession(@RequestHeader("Authorization") String authHeader) {
		
		// 검증1: 헤더가 없거나 형식이 Bearer 아닐 시 401 에러(No token provided)
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			return ResponseEntity.status(401).body("Unauthorized: No token provided");
		}

		String token = authHeader.substring(7);
		
		// 검증2: jwtUtil.validateToken(token)이 실패하면 401 (Invalid token)
		if (!jwtUtil.validateToken(token)) {
			return ResponseEntity.status(401).body("Unauthorized: Invalid token");
		}
		
		// 검증 완료: 사용자 식별(이메일) 확보
		String email = jwtUtil.extractEmail(token);

		// 세션 ID 생성
		String sessionID = UUID.randomUUID().toString();
		activeSessions.put(sessionID, email);

		Map<String, String> response = new HashMap<>();
		response.put("sessionId", sessionID);
		response.put("email", email);

		return ResponseEntity.ok(response);
		
	}

	/**
	 * 해당 세션ID가 유효한지 조회하는 엔드포인트 메소드
	 * 
	 * - sessionID로 activeSessions 에 저장된 매핑을 조회
	 * - 존재하면 세션 상세(여기서는 email만) 반환
	 * ㄴ 없을 시 404 Not Found 를 반환
	 * 
	 * @param sessionID
	 * @return sessionID로 activeSessions 에 저장된 매핑을 조회하여 존재시 세션 상세 반환
	 * 			없을 시 404 Not Found 반환
	 */
	@GetMapping("/{sessionId}")
	public ResponseEntity<?> getSession(@PathVariable String sessionID) {
		
		if (!activeSessions.containsKey(sessionID))
			return ResponseEntity.status(404).body("Session not found");

		Map<String, String> response = new HashMap<>();
		response.put("sessionId", sessionID);
		response.put("email", activeSessions.get(sessionID));

		return ResponseEntity.ok(response);
		
	}
	
}
