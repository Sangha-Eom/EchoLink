package com.EchoLink.auth_server.controller;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import com.EchoLink.auth_server.service.FirebaseAuthService;

/**
 * 
 */
@RestController
@RequestMapping("/api/firebase")
public class FirebaseController {

    private final FirebaseAuthService firebaseAuthService;

    public FirebaseController(FirebaseAuthService firebaseAuthService) {
        this.firebaseAuthService = firebaseAuthService;
    }

    /**
     * 클라이언트로부터 Google ID 토큰을 받아 Firebase Custom Token을 발급합니다.
     * @param idToken Google 로그인 후 받은 ID Token
     * @return 생성된 Firebase Custom Token
     */
    @PostMapping("/signin")
    public ResponseEntity<?> createCustomToken(@RequestBody Map<String, String> payload) {
        String idToken = payload.get("idToken");
        if (idToken == null) {
            return ResponseEntity.badRequest().body("ID token is missing.");
        }

        try {
            // 1. 수신된 Google ID 토큰 검증
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String uid = decodedToken.getUid();

            // 2. 검증된 사용자 UID로 Firebase Custom Token 생성
            String customToken = firebaseAuthService.createFirebaseCustomToken(uid);

            return ResponseEntity.ok(Map.of("customToken", customToken));

        } catch (Exception e) {
            return ResponseEntity.status(401).body("Token verification failed: " + e.getMessage());
        }
    }
}