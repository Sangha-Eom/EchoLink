package com.EchoLink.auth_server.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.SetOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * 기기 코드 관리 서비스 클래스
 * 
 * 서버(데스크톱) 등록 관리
 * Firebase 데이터베이스 사용
 * @author ESH
 */
@Service
public class DeviceService {


	private final Firestore db;

	public DeviceService() {
		this.db = FirestoreClient.getFirestore();
	}


	/**
	 * 특정 사용자의 온라인 상태인 기기 목록을 Firestore에서 조회합니다.
	 * @param userEmail 사용자의 이메일
	 * @return 온라인 기기 목록
	 * @throws FirebaseAuthException 
	 */
	public List<Map<String, Object>> getOnlineDevicesForUser(String userEmail) throws ExecutionException, InterruptedException, FirebaseAuthException {
		String uid = FirebaseAuth.getInstance().getUserByEmail(userEmail).getUid();

		ApiFuture<QuerySnapshot> future = db.collection("users").document(uid)
				.collection("devices")
				.whereEqualTo("status", "ONLINE")
				.get();

		List<QueryDocumentSnapshot> documents = future.get().getDocuments();
		List<Map<String, Object>> onlineDevices = new ArrayList<>();
		for (QueryDocumentSnapshot document : documents) {
			onlineDevices.add(document.getData());
		}
		return onlineDevices;
	}

	/**
	 * 기기의 상태를 업데이트합니다 (Heartbeat).
	 * @param userEmail 사용자의 이메일
	 * @param deviceName 기기 이름
	 * @param ipAddress IP 주소
	 * @throws FirebaseAuthException 
	 */
	public void updateDeviceStatus(String userEmail, String deviceName, String ipAddress) throws ExecutionException, InterruptedException, FirebaseAuthException {
		String uid = FirebaseAuth.getInstance().getUserByEmail(userEmail).getUid();
		var deviceDocRef = db.collection("users").document(uid)
				.collection("devices").document(deviceName);

		Map<String, Object> deviceData = new HashMap<>();
		deviceData.put("deviceName", deviceName);
		deviceData.put("ipAddress", ipAddress);
		deviceData.put("status", "ONLINE");
		deviceData.put("lastSeen", System.currentTimeMillis());

		// 문서가 존재하면 업데이트, 없으면 생성 (upsert)
		deviceDocRef.set(deviceData, com.google.cloud.firestore.SetOptions.merge()).get();
	}

	/**
	 * 특정 기기의 상태를 OFFLINE으로 변경합니다.
	 * @param userEmail 사용자의 이메일
	 * @param deviceName 기기 이름
	 * @throws FirebaseAuthException 
	 */
	public void setDeviceOffline(String userEmail, String deviceName) throws ExecutionException, InterruptedException, FirebaseAuthException {
		String uid = FirebaseAuth.getInstance().getUserByEmail(userEmail).getUid();
		var deviceDocRef = db.collection("users").document(uid)
				.collection("devices").document(deviceName);

		if (deviceDocRef.get().get().exists()) {
			deviceDocRef.update("status", "OFFLINE").get();
		}
	}

    /**
     * 사용자의 Refresh Token을 Firestore에 저장 (upsert).
     * @param uid 사용자의 고유 ID
     * @param deviceName 기기 이름
     * @param refreshToken 저장할 Refresh Token
     */
    public void storeRefreshToken(String uid, String deviceName, String refreshToken) throws Exception {
        var deviceDocRef = db.collection("users").document(uid)
                             .collection("devices").document(deviceName);
        
        Map<String, Object> tokenData = new HashMap<>();
        tokenData.put("refreshToken", refreshToken);

        deviceDocRef.set(tokenData, SetOptions.merge()).get();
    }

	/**
	 * 사용자의 Refresh Token을 Firestore에서 가져오기
	 */
	public String getRefreshToken(String userEmail, String deviceName) throws Exception {
		String uid = FirebaseAuth.getInstance().getUserByEmail(userEmail).getUid();
		var doc = db.collection("users").document(uid)
				.collection("devices").document(deviceName).get().get();

		if (doc.exists()) {
			return doc.getString("refreshToken");
		}
		return null;
	}

}
