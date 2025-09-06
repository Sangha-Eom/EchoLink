package com.EchoLink.auth_server.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
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
     * GoogleIdToken을 기반으로 사용자를 처리합니다.
     * @param idToken 검증된 Google ID 토큰
     * @return 처리된 사용자의 UserRecord
     */
    public UserRecord processUserLogin(GoogleIdToken idToken) throws ExecutionException, InterruptedException, FirebaseAuthException {
        GoogleIdToken.Payload payload = idToken.getPayload();
        String uid = payload.getSubject(); // Google의 고유 ID는 'sub' 필드.
        String email = payload.getEmail();
        String name = (String) payload.get("name");

        return findOrCreateFirebaseUser(uid, email, name, payload.getEmailVerified());
    }
	
    /**
     * FirebaseToken을 기반으로 사용자를 처리합니다.
     * @param decodedToken 검증된 Firebase ID 토큰
     * @return 처리된 사용자의 UserRecord
     */
	public UserRecord processUserLogin(FirebaseToken decodedToken) throws ExecutionException, InterruptedException, FirebaseAuthException {

		String uid = decodedToken.getUid();
		String email = decodedToken.getEmail();
		String name = decodedToken.getName();
		
		return findOrCreateFirebaseUser(uid, email, name, decodedToken.isEmailVerified());
    }
	
	
    /**
     * 사용자 조회 및 생성, Firestore 업데이트 로직 통합.
     */
    private UserRecord findOrCreateFirebaseUser(String uid, String email, String name, boolean isEmailVerified) throws FirebaseAuthException, ExecutionException, InterruptedException {
        UserRecord userRecord;
        try {
        	// 데이터베이스 안 사용자 존재 O 
            userRecord = FirebaseAuth.getInstance().getUser(uid);
        } catch (FirebaseAuthException e) {
        	// 데이터베이스 안 사용자 존재 X
            if ("USER_NOT_FOUND".equals(e.getAuthErrorCode().toString())) {
                System.out.println("새로운 사용자를 Firebase Authentication에 생성합니다: " + email);
                UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                        .setUid(uid)
                        .setEmail(email)
                        .setDisplayName(name)
                        .setEmailVerified(isEmailVerified);
                userRecord = FirebaseAuth.getInstance().createUser(request);
            } else {
                throw e;
            }
        }

        // Firestore 정보 업데이트
        var userDocRef = db.collection("users").document(uid);
        if (!userDocRef.get().get().exists()) {
            Map<String, Object> newUser = new HashMap<>();
            newUser.put("email", email);
            newUser.put("name", name);
            newUser.put("createdAt", System.currentTimeMillis());
            userDocRef.set(newUser).get();
            System.out.println("새로운 사용자 정보를 Firestore에 저장했습니다: " + email);
        } else {
            userDocRef.update("lastLoginAt", System.currentTimeMillis()).get();
        }

        return userRecord;
    }
}
