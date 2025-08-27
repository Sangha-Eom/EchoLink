package com.EchoLink.auth_server.controller;

import java.net.InetAddress;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import com.EchoLink.auth_server.service.Device;
import com.EchoLink.auth_server.service.DeviceService;
import com.EchoLink.auth_server.util.JwtUtil;

/**
 * 사용자 정보 API 컨트롤러 클래스(/api/devices/me)
 * 
 * 구글 로그인 → JWT 토큰 발급 
 * → 클라이언트(앱/데스크탑)가 JWT를 Authorization 헤더에 담아 호출 
 * → 서버에서 토큰 검증 후 사용자 이메일 반환
 * @author ESH
 */
@RestController
@RequestMapping("/api/devices")
public class DeviceController {
	
	private final DeviceService deviceService;
	private final JwtUtil jwtUtil;

	/**
	 * 생성자
	 * @param deviceService
	 */
	public DeviceController(JwtUtil jwtUtil, DeviceService deviceService) {
		this.jwtUtil = jwtUtil;
		this.deviceService = deviceService;
	}


	/**
	 * 로그인된 유저 정보 조회 메소드
	 * 
     * 클라이언트는 Authorization 헤더에 Bearer 토큰을 담아서 유저 정보 요청
	 * @param authHeader
	 * @return
	 */
	@GetMapping("/me")
	public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
		
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			return ResponseEntity.status(401).body("Unauthorized: No token provided");
		}

		String token = authHeader.substring(7); // "Bearer " 제거

		if (!jwtUtil.validateToken(token)) {
			return ResponseEntity.status(401).body("Unauthorized: Invalid token");
		}

		String email = jwtUtil.extractEmail(token);
		
		return ResponseEntity.ok().body("{\"email\":\"" + email + "\"}");
	}
	
	
    /**
     * Getter
     * 현재 로그인한 사용자의 온라인 기기 목록을 반환
     */
    @GetMapping
    public ResponseEntity<?> getMyDevices(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            return ResponseEntity.status(401).body("Unauthorized: Invalid token");
        }
        String email = jwtUtil.extractEmail(token);
        List<Device> devices = deviceService.getOnlineDevicesForUser(email);
        return ResponseEntity.ok(devices);
    }
    
    /**
     * 데스크톱 서버가 자신의 상태를 알리는 'heartbeat' 엔드포인트.
     */
    @PostMapping("/heartbeat")
    public ResponseEntity<?> deviceHeartbeat(@RequestHeader("Authorization") String authHeader, 
                                             @RequestBody Map<String, String> payload,
                                             HttpServletRequest request) {
        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        String email = jwtUtil.extractEmail(token);
        String deviceName = payload.get("deviceName");
        String clientIp = request.getRemoteAddr(); // 요청 IP 주소 자동 추출

        deviceService.updateDeviceStatus(email, deviceName, clientIp);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 데스크톱 서버가 종료될 때 호출하는 'shutdown' 엔드포인트.
     */
    @PostMapping("/shutdown")
    public ResponseEntity<?> deviceShutdown(@RequestHeader("Authorization") String authHeader,
                                            @RequestBody Map<String, String> payload) {
        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        String email = jwtUtil.extractEmail(token);
        String deviceName = payload.get("deviceName");

        deviceService.setDeviceOffline(email, deviceName);
        return ResponseEntity.ok().build();
    }
	
}