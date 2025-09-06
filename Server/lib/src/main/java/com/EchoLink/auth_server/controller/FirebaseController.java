package com.EchoLink.auth_server.controller;


import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

import com.EchoLink.auth_server.service.DeviceService;
import com.EchoLink.auth_server.service.FirebaseAuthService;
import com.EchoLink.auth_server.service.JwtTokenProvider;
import com.EchoLink.auth_server.service.UserService;

/**
 * 데이터베이스(firebase) 로그인 컨트롤러
 */
@RestController
@RequestMapping("/api/firebase")
public class FirebaseController {

	private final FirebaseAuthService firebaseAuthService;
	private final JwtTokenProvider jwtTokenProvider;
	private final DeviceService deviceService;
	private final UserService userService;	// 데이터베이스에서 유저 검색

	private final String googleClientId;	// application.yml의 client-id

	/**
	 * 생성자
	 * @param firebaseAuthService
	 * @param jwtTokenProvider
	 * @param deviceService
	 * @param googleClientId
	 */
	public FirebaseController(FirebaseAuthService firebaseAuthService,
			JwtTokenProvider jwtTokenProvider,
			DeviceService deviceService,
			UserService userService,
			@Value("${spring.security.oauth2.client.registration.google.client-id}") String googleClientId) {
		this.firebaseAuthService = firebaseAuthService;
		this.jwtTokenProvider = jwtTokenProvider;
		this.deviceService = deviceService;
		this.userService = userService;
		this.googleClientId = googleClientId;
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
		String idTokenString = payload.get("idToken");
		String deviceName = payload.get("deviceName");	// 클라이언트로부터 기기 이름 수신

		// idToken null 체크
		if (idTokenString == null) {
			return ResponseEntity.badRequest().body("ID token is missing.");
		}


		try {
			// 1. 수신된 Google ID 토큰 검증
			GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
					.setAudience(Collections.singletonList(googleClientId))
					.build();

			GoogleIdToken idToken = verifier.verify(idTokenString);
			if (idToken == null) {
				return ResponseEntity.status(401).body("Invalid ID token.");
			}

			// Firebase Token 객체 변환 (UserService -> FirebaseToken)
			// audience 검증은 건너뜀(GoogleIdTokenVerifier가 수행)
			FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idTokenString, true); 
			
			// 2. 검증된 사용자 UID로 Firebase Custom Token 생성
            UserRecord userRecord = userService.processUserLogin(decodedToken);
            String uid = userRecord.getUid();
            String email = userRecord.getEmail();


			// 3. Custom, Access, Refresh Token 생성
			String customToken = firebaseAuthService.createFirebaseCustomToken(uid);
			String accessToken = jwtTokenProvider.createAccessToken(email);
			String refreshToken = jwtTokenProvider.createRefreshToken(email);

			// 4. DeviceService를 통해 Refresh Token 저장 (uid 전달)
			deviceService.storeRefreshToken(uid, deviceName, refreshToken);

			return ResponseEntity.ok(Map.of(
					"customToken", customToken,
					"accessToken", accessToken,
					"refreshToken", refreshToken
					));
		} 
		catch (Exception e) {
			return ResponseEntity.status(401).body("Token verification failed: " + e.getMessage());
		}
	}
}
