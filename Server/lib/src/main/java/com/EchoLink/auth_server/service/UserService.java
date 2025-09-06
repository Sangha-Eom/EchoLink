package com.EchoLink.auth_server.service;

import com.google.cloud.firestore.Firestore;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import org.springframework.stereotype.Service;


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * 첫 로그인 이용자 저장 클래스
 * 
 * 첫 로그인 시 사용자를 Firestore에 자동 저장
 * FirebaseApp 초기화 → Firestore 객체 생성 → UserService.java
 */
@Service
public class UserService {

	private final Firestore db;

	public UserService(Firestore firestore) {
		this.db = firestore;
	}

	/**
	 * FirebaseToken을 기반으로 Firestore에 사용자 정보를 저장하거나 업데이트 함.
	 * @param decodedToken Firebase Admin SDK로 검증된 사용자 토큰
	 * @throws FirebaseAuthException 
	 */
	public UserRecord processUserLogin(FirebaseToken decodedToken) throws ExecutionException, InterruptedException, FirebaseAuthException {

		String uid = decodedToken.getUid();
		String email = decodedToken.getEmail();
		String name = decodedToken.getName();
		
		
		UserRecord userRecord;
		// Firebase Authentication에 사용자가 존재하는지 확인
		try {
			// 사용자가 존재 O
			userRecord = FirebaseAuth.getInstance().getUser(uid);
		} catch (FirebaseAuthException e) {
			// 사용자가 존재 X
			if (e.getAuthErrorCode().toString().equals("USER_NOT_FOUND")) {
				UserRecord.CreateRequest request = new UserRecord.CreateRequest()
						.setUid(uid)
						.setEmail(email)
						.setDisplayName(name)
						.setEmailVerified(decodedToken.isEmailVerified()); // Google 토큰의 이메일 검증 상태를 따름
				userRecord = FirebaseAuth.getInstance().createUser(request);
				System.out.println("새로운 사용자를 Firebase Authentication에 생성했습니다: " + email);
			} else {
				throw e;
			}
		}


		// Firestore의 'users' 컬렉션에서 해당 UID의 문서가 있는지 확인
		var userDocRef = db.collection("users").document(uid);

		if (!userDocRef.get().get().exists()) {
			// 문서가 없으면(첫 로그인), 새 사용자 정보를 생성
			Map<String, Object> newUser = new HashMap<>();
			newUser.put("email", email);
			newUser.put("name", name);
			newUser.put("createdAt", System.currentTimeMillis());

			userDocRef.set(newUser).get();
			System.out.println("새로운 사용자 정보를 Firestore에 저장했습니다: " + email);
		} 
		else {
			// 기존 사용자의 경우, 마지막 로그인 시간 등 정보 업데이트
			userDocRef.update("lastLoginAt", System.currentTimeMillis()).get();
		}

		return userRecord;
	}
}
