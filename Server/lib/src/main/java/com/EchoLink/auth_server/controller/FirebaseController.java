package com.EchoLink.auth_server.controller;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import com.EchoLink.auth_server.service.DeviceService;
import com.EchoLink.auth_server.service.FirebaseAuthService;
import com.EchoLink.auth_server.service.JwtTokenProvider;

/**
 * 데이터베이스(firebase) 로그인 컨트롤러
 */
@RestController
@RequestMapping("/api/firebase")
public class FirebaseController {

    private final FirebaseAuthService firebaseAuthService;
    private final JwtTokenProvider jwtTokenProvider;
    private final DeviceService deviceService;

    public FirebaseController(FirebaseAuthService firebaseAuthService, JwtTokenProvider jwtTokenProvider, DeviceService deviceService) {
        this.firebaseAuthService = firebaseAuthService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.deviceService = deviceService;
    }

    /**
     * 클라이언트로부터 Google ID 토큰을 받아 Firebase Custom Token을 발급.
     * Access Token과 Refresh Token 생성 (email)
     * 
     * @param idToken Google 로그인 후 받은 ID Token
     * @return 생성된 Firebase Custom Token
     */
    @PostMapping("/signin")
    public ResponseEntity<?> createCustomToken(@RequestBody Map<String, String> payload) {
        String idToken = payload.get("idToken");
        String deviceName = payload.get("deviceName");	// 클라이언트로부터 기기 이름 수신
        
        if (idToken == null) {
            return ResponseEntity.badRequest().body("ID token is missing.");
        }

        try {
            // 1. 수신된 Google ID 토큰 검증
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String uid = decodedToken.getUid();
            String email = decodedToken.getEmail();

            // 2. 검증된 사용자 UID로 Firebase Custom Token 생성
            String customToken = firebaseAuthService.createFirebaseCustomToken(uid);
            
            // 3. Access Token, Refresh Token 생성
            String accessToken = jwtTokenProvider.createAccessToken(email);
            String refreshToken = jwtTokenProvider.createRefreshToken(email);
            
            // Refresh Token을 Firestore에 저장
            deviceService.storeRefreshToken(email, deviceName, refreshToken);
            
            return ResponseEntity.ok(Map.of(
                    "customToken", customToken,
                    "accessToken", accessToken,
                    "refreshToken", refreshToken
                ));
        } 
        catch (FirebaseAuthException e) {
        	return ResponseEntity.status(401).body("Token verification failed: " + e.getMessage());
        } 
        catch (Exception e) {
            return ResponseEntity.status(401).body("Token verification failed: " + e.getMessage());
        }
    }
}
