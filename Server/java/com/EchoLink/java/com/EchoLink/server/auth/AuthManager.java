package com.EchoLink.server.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * JWT(JSON Web Token)를 이용한 사용자 인증을 담당합니다.
 * @author ESH
 */
public class AuthManager {

    // 🚨 매우 중요: 이 비밀 키는 '인증 웹 서버'의 JwtUtil 클래스에 있는 키와
    //    반드시 동일해야 합니다. 향후 외부 설정 파일에서 안전하게 불러오도록 수정해야 합니다.
    private final SecretKey secretKey = Keys.hmacShaKeyFor(
        "your-super-secret-key-that-is-long-and-secure-enough-for-hs256".getBytes(StandardCharsets.UTF_8)
    );

    /**
     * JWT의 유효성을 검증하는 메소드.
     * @param jwt 클라이언트로부터 받은 JWT 문자열
     * @return 검증 성공 시 true, 실패 시 false
     */
    public boolean validateToken(String jwt) {
        if (jwt == null || jwt.trim().isEmpty()) {
            return false;
        }

        try {
            // Jwts.parser()를 사용하여 토큰을 파싱하고 서명을 검증합니다.
            // 서명이 유효하지 않거나 토큰이 만료되었으면 예외가 발생합니다.
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(jwt);
            
            // 예외가 발생하지 않으면 토큰이 유효한 것입니다.
            return true;

        } catch (Exception e) {
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