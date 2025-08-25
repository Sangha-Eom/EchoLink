package com.EchoLink.auth_server.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT 생성 및 검증 유틸리티 클래스
 * 
 * @author ESH
 */
@Component
public class JwtUtil {

	private SecretKey secretKey;

	/**
	 * 생성자
	 * @param secretKey application.yml에서 주입받은 secretKey 값
	 */
	public JwtUtil(@Value("${jwt.secret}") String secretKey) {
		this.secretKey = Keys.hmacShaKeyFor(secretKey.getBytes());
	}

	/**
	 * JWT 토큰 발급 메소드
	 * 
	 * @param username 토큰 발급 주체(사용자/이메일 등)
	 * @return JWT 토큰
	 */
	public String generateToken(String username) {

		long nowMillis = System.currentTimeMillis();
		long expMillis = nowMillis + (1000 * 60 * 60); // 1시간 유효

		return Jwts.builder()
				.subject(username) // 토큰의 주체 (사용자 이름/이메일 등)
				.issuedAt(new Date(nowMillis)) // 발급 시간
				.expiration(new Date(expMillis)) // 만료 시간 (1시간)
				.signWith(secretKey) // 비밀 키로 서명
				.compact();
	}

	/**
	 * JWT 토큰에서 Claims 파싱
	 * 
	 * @param token
	 * @return
	 */
	private Claims parseClaims(String token) {
		return Jwts.parser()
				.verifyWith(secretKey)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}

	/**
	 * jWT 토큰 유효성 검사 (만료 & 서명 확인)
	 * 
	 * @param username
	 * @return
	 */
	public Boolean validateToken(String token) {
		try {
			Claims claims = parseClaims(token);
			return !claims.getExpiration().before(new Date());
		} catch (Exception e) {
			return false;
		}
	}


	/**
	 * JWT 토큰에서 사용자(이메일, 사용자 이름 등(=sub)) 추출
	 * 
	 * @param email
	 * @return
	 */
	public String extractEmail(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.getSubject();	 // generateToken에서 setSubject(email) 했으므로 여기서 가져옴
        } catch (Exception e) {
            return null;
        }
	}
}