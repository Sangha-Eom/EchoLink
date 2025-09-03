package com.EchoLink.auth_server.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

/**
 *  Spring Boot 서버에 Google ID 토큰을 받아서 Firebase 맞춤 토큰(Custom Token)을 생성
 */
@Service
public class FirebaseAuthService {

    /**
     * UID를 기반으로 Firebase Custom Token을 생성합니다.
     * @param uid 사용자의 고유 ID
     * @return Firebase Custom Token 문자열
     */
    public String createFirebaseCustomToken(String uid) throws FirebaseAuthException {
        
        Map<String, Object> additionalClaims = new HashMap<>();
        // 추가적인 클레임(권한 등)이 필요 시 추가
        // additionalClaims.put("premium", true);

        return FirebaseAuth.getInstance().createCustomToken(uid, additionalClaims);
    }
}
