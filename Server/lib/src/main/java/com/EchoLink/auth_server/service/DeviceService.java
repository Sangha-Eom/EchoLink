package com.EchoLink.auth_server.service;

import com.google.common.base.Optional;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.EchoLink.auth_server.util.JwtUtil;

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
	// 사용자 이메일과 연결된 영구 기기 목록 저장소
	// { userEmail: [Device1, Device2, ...] }
	private final Map<String, List<Device>> userDevices = new ConcurrentHashMap<>();


	/**
	 * 생성자
	 * @param jwtUtil 자바 토큰 발급 인스턴스
	 */
	public DeviceService(JwtUtil jwtUtil) {
		this.jwtUtil = jwtUtil;
	}


	/**
	 *  1. 데스크톱 서버가 호출: 기기 등록 및 고유 코드 생성
	 * @return
	 */
	public String registerDevice() {
		String deviceCode = generateDeviceCode();
		deviceCodeStorage.put(deviceCode, "PENDING"); // 초기 상태는 PENDING
		return deviceCode;
	}

	/**
	 *  2. 웹에 로그인한 사용자가 호출: 코드를 통해 계정과 기기 연결
	 * @param deviceCode
	 * @param userEmail
	 * @return
	 */
	public boolean linkDevice(String deviceCode, String userEmail) {
		if (deviceCodeStorage.containsKey(deviceCode) && "PENDING".equals(deviceCodeStorage.get(deviceCode))) {
			deviceCodeStorage.put(deviceCode, userEmail); // PENDING 상태를 사용자 이메일로 변경
			return true;
		}
		return false;
	}

	/**
	 *  3. 데스크톱 서버가 주기적으로 호출: 인증 완료 및 JWT 요청
	 * @param deviceCode
	 * @return
	 */
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



	/**
	 * 특정 계정에 속한 온라인 상태의 기기 목록 조회.
	 */
	public List<Device> getOnlineDevicesForUser(String userEmail) {
		// 오프라인 기기 정리 (예: 5분 이상 응답 없는 기기)
		cleanupOfflineDevices(userEmail);

		return userDevices.getOrDefault(userEmail, new ArrayList<>())
				.stream()
				.filter(device -> "ONLINE".equals(device.getStatus()))
				.collect(Collectors.toList());
	}

	/**
	 * 데스크톱의 상태를 업데이트(heartbeat)
	 * 필요 시 새 기기를 등록합니다.
	 */
	public void updateDeviceStatus(String userEmail, String deviceName, String ipAddress) {
		userDevices.putIfAbsent(userEmail, new ArrayList<>());

		List<Device> devices = userDevices.get(userEmail);
		Device existingDevice = devices.stream()
				.filter(d -> deviceName.equals(d.getDeviceName()))
				.findFirst()
				.orElse(null);

		if (existingDevice != null) {
			// 기존 기기는 상태와 IP, 마지막 접속 시간을 업데이트
			existingDevice.setStatus("ONLINE");
			existingDevice.setIpAddress(ipAddress);
			existingDevice.setLastSeen(System.currentTimeMillis());
		} 
		else {
			// 새 기기 등록
			devices.add(new Device(deviceName, ipAddress));
		}
	}

	/**
	 * 오래된 오프라인 기기를 정리하는 내부 메소드
	 * 데스크탑 연결 비정상 종료 시 사용
	 */
	private void cleanupOfflineDevices(String userEmail) {
		if (!userDevices.containsKey(userEmail))
			return;

		long cutoff = System.currentTimeMillis() - (5 * 60 * 1000); // 5분
		userDevices.get(userEmail).forEach(device -> {
			if (device.getLastSeen() < cutoff) {
				device.setStatus("OFFLINE");
			}
		});
	}
	
    /**
     * 특정 기기의 상태를 명시적으로 오프라인으로 변경
     */
    public void setDeviceOffline(String userEmail, String deviceName) {
        if (!userDevices.containsKey(userEmail)) return;

        userDevices.get(userEmail).stream()
            .filter(d -> deviceName.equals(d.getDeviceName()))
            .findFirst()
            .ifPresent(device -> device.setStatus("OFFLINE"));
    }

	/*
	 *  간단한 6자리 랜덤 코드 생성
	 */
	private String generateDeviceCode() {
		SecureRandom random = new SecureRandom();
		int num = random.nextInt(1000000);
		return String.format("%06d", num);
	}


}