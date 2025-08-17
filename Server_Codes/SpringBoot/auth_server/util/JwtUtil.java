package com.EchoLink.auth_server.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 생성 및 검증 유틸리티 클래스
 */
@Component
public class JwtUtil {

    // 매우 중요: 이 비밀 키는 외부 설정(application.yml)으로 분리해야 합니다.
    private final SecretKey secretKey = Keys.hmacShaKeyFor(
        "your-super-secret-key-that-is-long-and-secure-enough-for-hs256".getBytes(StandardCharsets.UTF_8)
    );

    // JWT 생성 메소드
    public String generateToken(String username) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(username) // 토큰의 주체 (사용자 이름/이메일 등)
                .issuedAt(new Date(now)) // 발급 시간
                .expiration(new Date(now + 1000 * 60 * 60 * 24)) // 만료 시간 (예: 24시간)
                .signWith(secretKey) // 비밀 키로 서명
                .compact();
    }
}