package com.EchoLink.auth_server.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import com.EchoLink.auth_server.service.DeviceService;
import com.EchoLink.auth_server.service.JwtTokenProvider;

/**
 * 토큰 재발급 컨트롤러
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;	// 토큰 제공자
    private final DeviceService deviceService;			// 기기 관리자

    public AuthController(JwtTokenProvider jwtTokenProvider, DeviceService deviceService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.deviceService = deviceService;
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> payload) {
        String refreshToken = payload.get("refreshToken");
        String deviceName = payload.get("deviceName");

        try {
            // 1. 토큰 유효성 검증
            if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)) {
                return ResponseEntity.status(401).body("Invalid Refresh Token");
            }
            
            // 2. 토큰에서 사용자 정보 추출 및 DB에 저장된 토큰과 비교
            String userEmail = jwtTokenProvider.getSubject(refreshToken);
            String storedToken = deviceService.getRefreshToken(userEmail, deviceName);

            if (storedToken == null || !storedToken.equals(refreshToken)) {
                return ResponseEntity.status(401).body("Refresh Token mismatch or not found");
            }

            // 3. 새 Access Token 생성
            String newAccessToken = jwtTokenProvider.createAccessToken(userEmail);

            return ResponseEntity.ok(Map.of("accessToken", newAccessToken));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("An error occurred during token refresh: " + e.getMessage());
        }
    }
}
