package com.EchoLink.auth_server.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 생성, 검증, 정보 추출 전담 서비스 클래스
 */
@Service
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessTokenValidityInMilliseconds;
    private final long refreshTokenValidityInMilliseconds;

    public JwtTokenProvider(@Value("${jwt.secret}") String secretKey,
                              @Value("${jwt.access-token-expiration-ms}") long accessTokenValidity,
                              @Value("${jwt.refresh-token-expiration-ms}") long refreshTokenValidity) {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidityInMilliseconds = accessTokenValidity;
        this.refreshTokenValidityInMilliseconds = refreshTokenValidity;
    }

    // Access Token 생성
    public String createAccessToken(String subject) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + accessTokenValidityInMilliseconds);
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key)
                .compact();
    }

    // Refresh Token 생성
    public String createRefreshToken(String subject) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + refreshTokenValidityInMilliseconds);
        return Jwts.builder()
                .setSubject(subject) // Refresh Token에는 최소한의 정보만 담는 것이 좋습니다.
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key)
                .compact();
    }

    // 토큰에서 Subject(사용자 이메일) 추출
    public String getSubject(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    // 토큰 유효성 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            // ExpiredJwtException, UnsupportedJwtException, MalformedJwtException, SignatureException, IllegalArgumentException
            return false;
        }
    }
}
