package com.EchoLink.auth_server.controller;

import com.EchoLink.auth_server.util.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 사용자 정보 API 컨트롤러 클래스(/api/devices/me)
 * 
 * 구글 로그인 → JWT 토큰 발급 
 * → 클라이언트(앱/데스크탑)가 JWT를 Authorization 헤더에 담아 호출 
 * → 서버에서 토큰 검증 후 사용자 이메일 반환
 * @author ESH
 */
@RestController
@RequestMapping("/api/devices")
public class DeviceController {

	private final JwtUtil jwtUtil;

	/**
	 * 생성자
	 * @param deviceService
	 */
	public DeviceController(JwtUtil jwtUtil) {
		this.jwtUtil = jwtUtil;
	}


	/**
	 * 로그인된 유저 정보 조회 메소드
	 * 
     * 클라이언트는 Authorization 헤더에 Bearer 토큰을 담아서 유저 정보 요청
	 * @param authHeader
	 * @return
	 */
	@GetMapping("/me")
	public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
		
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			return ResponseEntity.status(401).body("Unauthorized: No token provided");
		}

		String token = authHeader.substring(7); // "Bearer " 제거

		if (!jwtUtil.validateToken(token)) {
			return ResponseEntity.status(401).body("Unauthorized: Invalid token");
		}

		String email = jwtUtil.extractEmail(token);
		
		return ResponseEntity.ok().body("{\"email\":\"" + email + "\"}");
		
	}
}