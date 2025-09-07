package com.EchoLink.server.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;

/**
 * SpringBoot 시작 시 Firebase 서비스 초기화하는 클래스
 */
@Configuration
public class FirebaseConfig {

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        // 현재 초기화된 FirebaseApp이 있는지 확인
        List<FirebaseApp> firebaseApps = FirebaseApp.getApps();
        if (firebaseApps != null && !firebaseApps.isEmpty()) {
            for (FirebaseApp app : firebaseApps) {
                if (app.getName().equals(FirebaseApp.DEFAULT_APP_NAME)) {
                    return app; // 이미 기본 앱이 초기화되었으면 해당 인스턴스 반환
                }
            }
        }

        // 비공개 키 파일 로드
        InputStream serviceAccount = new FileInputStream("secret/serviceAccountKey.json");

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

        return FirebaseApp.initializeApp(options);
    }
    
    /**
     * Firestore 객체를 Bean으로 등록
     * @param firebaseApp
     * @return
     */
    @Bean
    public Firestore firestore(FirebaseApp firebaseApp) {
        // 이 메소드는 firebaseApp()이 성공적으로 실행된 후에만 호출 됨.
        return FirestoreClient.getFirestore(firebaseApp);
    }
}
