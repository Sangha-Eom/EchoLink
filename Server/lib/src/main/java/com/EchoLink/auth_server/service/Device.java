package com.EchoLink.auth_server.service;

public class Device {

	private String deviceName;	// 기기 이름
	private String ipAddress;	// ip 주소
	private long lastConnect; // 마지막으로 온라인이었던 시간 (Timestamp)
	private String status; // 상태 "ONLINE" or "OFFLINE"


	/**
	 * 생성자
	 * 
	 * @param deviceName 기기 이름
	 * @param ipAddress	ip주소
	 */
	public Device(String deviceName, String ipAddress) {
		this.deviceName = deviceName;
		this.ipAddress = ipAddress;
		this.status = "ONLINE";
		this.lastConnect = System.currentTimeMillis();
	}

	
	/**
	 * Getter
	 * @return 디바이스 이름
	 */
	public String getDeviceName() {
		return deviceName; 
	}
	/**
	 * Getter
	 * @return IP주소
	 */
	public String getIpAddress() {
		return ipAddress; 
	}
	/**
	 * Getter
	 * @return 마지막 연결 시간
	 */
	public long getLastSeen() {
		return lastConnect; 
	}
	/**
	 * Getter
	 * @return 현재 상태
	 */
	public String getStatus() {
		return status; 
	}

	/**
	 * Setter
	 * @param deviceName 디바이스 이름
	 */
	public void setDeviceName(String deviceName) { 
		this.deviceName = deviceName; 
	}
	/**
	 * Setter
	 * @param ipAddress IP주소
	 */
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress; 
	}
	/**
	 * Setter
	 * @param lastSeen 마지막 접속 시간
	 */
	public void setLastSeen(long lastSeen) {
		this.lastConnect = lastSeen; 
	}
	/**
	 * Setter
	 * @param status 현재 상태
	 */
	public void setStatus(String status) {
		this.status = status; 
	}
}