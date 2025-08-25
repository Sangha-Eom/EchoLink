package com.EchoLink.server.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

public class AuthManager {

    // TODO: 인증 서버의 application.yml에 있는 jwt.secret 값 불러오기
    private final SecretKey secretKey = Keys.hmacShaKeyFor(
            "C0MR4qiaubVrckcgmAQ1iEoBI5KPPPyn".getBytes(StandardCharsets.UTF_8)
    );


    /**
     * 인증 서버의 토큰과 클라이언트의 토큰이 유효하고 일치하는지 검증하는 메소드
     * 
     * @param serverToken 인증 서버 토큰
     * @param clientToken 클라이언트 토큰
     * @return
     */
    public boolean validateToken(String serverToken, String clientToken) {
        if (serverToken == null || clientToken == null || !serverToken.equals(clientToken)) {
            return false;
        }
        try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(clientToken);
            return true;
        } catch (Exception e) {
            System.err.println("JWT 검증 실패: " + e.getMessage());
            return false;
        }
    }

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
  