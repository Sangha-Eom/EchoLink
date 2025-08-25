package com.EchoLink.auth_server.service;

import com.EchoLink.auth_server.util.JwtUtil;
import com.google.common.base.Optional;

import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 기기 코드 관리 서비스 클래스
 * 
 * 서버(데스크톱) 등록 관리
 * 발급된 코드 임시로 저장.
 * @author ESH
 */
@Service
public class DeviceService {
	

    private final JwtUtil jwtUtil;
    // 메모리 기반 저장소: { deviceCode: userEmail }
    private final Map<String, String> deviceCodeStorage = new ConcurrentHashMap<>();
    // 메모리 기반 저장소: { deviceCode: jwtToken }
    private final Map<String, String> tokenStorage = new ConcurrentHashMap<>();
	
	/**
	 * 생성자
	 * @param jwtUtil 자바 토큰 발급 인스턴스
	 */
	public DeviceService(JwtUtil jwtUtil) {
		this.jwtUtil = jwtUtil;
	}


    // 1. 데스크톱 서버가 호출: 기기 등록 및 고유 코드 생성
    public String registerDevice() {
        String deviceCode = generateDeviceCode();
        deviceCodeStorage.put(deviceCode, "PENDING"); // 초기 상태는 PENDING
        return deviceCode;
    }

    // 2. 웹에 로그인한 사용자가 호출: 코드를 통해 계정과 기기 연결
    public boolean linkDevice(String deviceCode, String userEmail) {
        if (deviceCodeStorage.containsKey(deviceCode) && "PENDING".equals(deviceCodeStorage.get(deviceCode))) {
            deviceCodeStorage.put(deviceCode, userEmail); // PENDING 상태를 사용자 이메일로 변경
            return true;
        }
        return false;
    }

    // 3. 데스크톱 서버가 주기적으로 호출: 인증 완료 및 JWT 요청
    public String pollDeviceStatus(String deviceCode) {
        String userEmail = deviceCodeStorage.get(deviceCode);

        // 아직 PENDING 상태이거나 코드가 없으면 null 반환
        if (userEmail == null || "PENDING".equals(userEmail)) {
            return null;
        }

        // 이미 토큰이 발급되었으면 기존 토큰 반환
        if (tokenStorage.containsKey(deviceCode)) {
            return tokenStorage.get(deviceCode);
        }

        // 사용자 이메일로 JWT 생성
        String token = jwtUtil.generateToken(userEmail);
        tokenStorage.put(deviceCode, token); // 발급된 토큰 저장
        return token;
    }


    // 간단한 6자리 랜덤 코드 생성
    private String generateDeviceCode() {
        SecureRandom random = new SecureRandom();
        int num = random.nextInt(1000000);
        return String.format("%06d", num);
    }
    

}