package com.EchoLink.server.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * JWT(JSON Web Token)를 이용한 사용자 인증을 담당하는 클래스
 * 
 * @author ESH
 */
public class AuthManager {

	// 🚨 매우 중요: 이 비밀 키는 '인증 웹 서버'의 JwtUtil 클래스에 있는 키와 반드시 동일해야 함.
	//    TODO: 향후 외부 설정 파일에서 안전하게 불러오도록 수정해야 합니다.
	private final SecretKey secretKey = Keys.hmacShaKeyFor(
			"C0MR4qiaubVrckcgmAQ1iEoBI5KPPPyn".getBytes(StandardCharsets.UTF_8)
			);

	/**
	 * 서버에 저장된 토큰과 클라이언트 토큰의 유효성과 일치성을 검증.
	 * 
	 * @param serverToken 서버가 인증 서버로부터 받은 원본 JWT
	 * @param clientToken 클라이언트가 연결을 제시한 JWT
	 * @return 검증(유효,일치) 성공 시 true, 실패 시 false
	 */
	public boolean validateToken(String serverToken, String clientToken) {

		if (serverToken == null || clientToken == null || !serverToken.equals(clientToken)) {
			return false;
		}

		try {
			// Jwts.parser()를 사용하여 토큰을 파싱하고 유효성(서명, 만료 기간 등)을 검증.
			// 서명이 유효하지 않거나 토큰이 만료되었으면 예외가 발생.
			Jwts.parser()
				.verifyWith(secretKey)
				.build()
				.parseSignedClaims(clientToken);

			// 예외가 발생하지 않으면 토큰이 유효한 것입니다.
			return true;

		} 
		catch (Exception e) {
			System.err.println("JWT 검증 실패: " + e.getMessage());
			return false;
		}
	}

	/** (선택)
	 * 토큰에서 사용자 정보(이메일 등)를 추출하는 메소드
	 */
	public String getUsernameFromToken(String jwt) {
		try {
			return Jwts.parser()
					.verifyWith(secretKey)
					.build()
					.parseSignedClaims(jwt)
					.getPayload()
					.getSubject();
		} catch (Exception e) {
			return "unknown";
		}
	}
}