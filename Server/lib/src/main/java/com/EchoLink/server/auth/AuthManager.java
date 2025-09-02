package com.EchoLink.server.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class AuthManager {

    private final SecretKey secretKey;
    
    public AuthManager(String jwtSecret) {
    	
    	if (jwtSecret == null || jwtSecret.isBlank()) {
    		throw new IllegalArgumentException("JWT secret key cannot be null or empty.");
        }
    	
    	this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)
        );
	}

    /**
     * 인증 서버의 토큰과 클라이언트의 토큰이 유효하고 일치하는지 검증하는 메소드
     * 
     * @param serverToken 인증 서버 토큰
     * @param clientToken 클라이언트 토큰
     * @return
     */
    public boolean validateToken(String serverToken, String clientToken) {
        if (clientToken == null) {
            return false;
        }
        try {
            Jws<Claims> claimsJws = Jwts.parser()
                                        .verifyWith(secretKey)
                                        .build()
                                        .parseSignedClaims(clientToken);

            // 만료 시간 검사 (현재 시간보다 이전이면 만료된 것)
            return !claimsJws.getPayload().getExpiration().before(new Date());
        } catch (Exception e) {
            System.err.println("JWT 검증 실패: " + e.getMessage());
            return false;
        }
    }

    /**
     * JWT에서 사용자 이메일(Subject)을 추출하는 메소드
     * @param jwt
     * @return
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
  
