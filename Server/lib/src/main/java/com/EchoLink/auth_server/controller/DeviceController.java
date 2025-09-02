package com.EchoLink.auth_server.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import com.EchoLink.auth_server.service.DeviceService;

/**
 * 사용자 정보 API 컨트롤러 클래스(/api/devices/me)
 * 
 * 구글 로그인 → Firebase토큰에서 사용자 정보를 가져와 인증(DeviceService.java)
 * @author ESH
 */
@RestController
@RequestMapping("/api/devices")
public class DeviceController {

	private final DeviceService deviceService;

	/**
	 * 생성자
	 * @param deviceService
	 */
	public DeviceController(DeviceService deviceService) {
		this.deviceService = deviceService;
	}


	/**
	 * 현재 로그인한 사용자의 온라인 기기 목록을 반환합니다.
	 */
	@GetMapping
	public ResponseEntity<?> getMyDevices(Authentication authentication) {
		try {
			// SecurityContext에서 인증된 사용자의 이메일을 가져옵니다.
			String email = authentication.getName();
			List<Map<String, Object>> devices = deviceService.getOnlineDevicesForUser(email);
			return ResponseEntity.ok(devices);
		} catch (Exception e) {
			return ResponseEntity.internalServerError().body("기기 목록을 가져오는 중 오류 발생: " + e.getMessage());
		}
	}

	/**
	 * 데스크톱 서버가 자신의 상태를 알리는 'heartbeat' 엔드포인트입니다.
	 */
	@PostMapping("/heartbeat")
	public ResponseEntity<?> deviceHeartbeat(Authentication authentication,
			@RequestBody Map<String, String> payload,
			HttpServletRequest request) {
		try {
			String email = authentication.getName();
			String deviceName = payload.get("deviceName");
			String clientIp = request.getRemoteAddr();

			deviceService.updateDeviceStatus(email, deviceName, clientIp);
			return ResponseEntity.ok().build();
		} catch (Exception e) {
			return ResponseEntity.internalServerError().body("Heartbeat 처리 중 오류 발생: " + e.getMessage());
		}
	}

	/**
	 * 데스크톱 서버가 종료될 때 호출하는 'shutdown' 엔드포인트입니다.
	 */
	@PostMapping("/shutdown")
	public ResponseEntity<?> deviceShutdown(Authentication authentication,
			@RequestBody Map<String, String> payload) {
		try {
			String email = authentication.getName();
			String deviceName = payload.get("deviceName");

			deviceService.setDeviceOffline(email, deviceName);
			return ResponseEntity.ok().build();
		} catch (Exception e) {
			return ResponseEntity.internalServerError().body("Shutdown 처리 중 오류 발생: " + e.getMessage());
		}
	}
}