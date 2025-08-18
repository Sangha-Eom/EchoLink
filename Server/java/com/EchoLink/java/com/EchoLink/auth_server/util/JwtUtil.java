package com.EchoLink.auth_server.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 생성 및 검증 유틸리티 클래스
 * @author ESH
 */
@Component
public class JwtUtil {


    @Value("${jwt.secret}") 	// application.yml에서 값을 주입받음
    private String secretString;
    
    private SecretKey secretKey;

    @PostConstruct // 의존성 주입이 완료된 후 실행되는 메소드
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(secretString.getBytes(StandardCharsets.UTF_8));
    }

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