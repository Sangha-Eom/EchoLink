package com.EchoLink.server.gui;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.firebase.auth.FirebaseAuth;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.awt.Desktop;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.IOException;

import com.EchoLink.auth_server.FxApplication;
import com.EchoLink.server.gui.GuiService;

/**
 * 구글 로그인 창 컨트롤러
 * 
 * 시스템의 기본 웹 브라우저를 열어 구글 로그인을 진행
 * 인증이 완료되면 Firebase ID 토큰을 받아 옴
 * (로컬 서버를 잠시 열어 구글의 인증 콜백을 받는 표준 데스크톱 OAuth2 방식 사용)
 */
@Component
public class LoginController {

	@FXML private Button loginButton;
	@FXML private Label statusLabel;
	
	private GuiService guiService; // GuiService 멤버 변수
	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	private final HttpClient httpClient = HttpClient.newHttpClient();

	// Firebase 콘솔에서 발급받은 웹 클라이언트의 JSON 파일을 사용(client_secret.json)
	private static final String CREDENTIALS_FILE_PATH = "/client_secret.json";
	private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
	private static final List<String> SCOPES = Arrays.asList("email", "profile");

    private final String FIREBASE_WEB_API_KEY; 
    
    // 어플에서 사용할 고정 포트 번호
    private static final int LOCAL_REDIRECT_PORT = 57323;
	
    /**
     * 생성자
     * @param firebaseApiKey
     */
    public LoginController(@Value("${echolink.firebase.web-api-key}") String firebaseApiKey) {
        this.FIREBASE_WEB_API_KEY = firebaseApiKey;
    }
    
    
    public void setGuiService(GuiService guiService) {
        this.guiService = guiService;
    }

	@FXML
	private void handleGoogleLogin() {
		
		// application.yml 에서 Firebase web api key 검증
        if (FIREBASE_WEB_API_KEY == null || FIREBASE_WEB_API_KEY.isEmpty() || "YOUR_FIREBASE_WEB_API_KEY".equals(FIREBASE_WEB_API_KEY)) {
            showStatus("오류: Firebase 웹 API 키가 application.yml에 설정되지 않았습니다.");
            return;
        }
        
		loginButton.setDisable(true);
		showStatus("Google 로그인을 시작합니다...");

		// 별도의 스레드에서 인증 과정을 진행 -> GUI가 멈추지 않도록 함
		executor.submit(this::performGoogleSignIn);
	}


	private void performGoogleSignIn() {
		try {
			final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

			// client_secret.json 파일 로드
			GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
					new InputStreamReader(LoginController.class.getResourceAsStream(CREDENTIALS_FILE_PATH)));

			// 로컬 콜백을 받을 포트 자동 할당
            int localPort = findFreePort();
            String redirectUri = "http://127.0.0.1:" + localPort;

			// Google 인증 흐름(Flow) 설정
			GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
					httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
					.build();

			// 인증 URL 생성 및 브라우저 열기
			String authorizationUrl = flow.newAuthorizationUrl().setRedirectUri(redirectUri).build();
			Desktop.getDesktop().browse(new URI(authorizationUrl));
			showStatus("브라우저에서 Google 로그인을 완료해주세요...");

			// 로컬 서버로 리디렉션(콜백) 대기
			// TODO: 이 부분은 실제 프로덕션에서는 더 정교한 로컬 서버 구현이 필요합니다.
			// 여기서는 간단한 임시 서버 로직을 사용합니다.
			String authCode = new LocalCallbackServer(localPort).waitForCode();

			if (authCode != null) {
				showStatus("인증 코드를 받았습니다. 토큰을 확인합니다...");
				// 받은 인증 코드로 Google Credential 생성
				GoogleTokenResponse tokenResponse = flow.newTokenRequest(authCode).setRedirectUri(redirectUri).execute();

				if (tokenResponse != null) {
					showStatus("인증 코드를 받았습니다. 토큰을 확인합니다...");

					// GoogleTokenResponse에서 ID 토큰을 추출할 수 있습니다.
					String googleIdToken = tokenResponse.getIdToken();
					
					String customToken = getFirebaseCustomToken(googleIdToken);
					signInWithFirebaseRest(customToken);

					showStatus("로그인 성공!");

				} else {
					showStatus("로그인에 실패했거나 취소되었습니다.");
					Platform.runLater(() -> loginButton.setDisable(false));
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			showStatus("오류 발생: " + e.getMessage());
			Platform.runLater(() -> loginButton.setDisable(false));
		}
	}
	
	/**
	 *  Custom Token으로 Firebase에 로그인하고 최종 ID 토큰을 얻는 메소드
	 * @param customToken
	 * @throws Exception
	 */
    private void signInWithFirebaseRest(String customToken) throws Exception {
    	
        showStatus("Firebase에 최종 로그인합니다...");
        String requestBody = new JSONObject(Map.of("token", customToken, "returnSecureToken", true)).toString();

        String firebaseUri = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithCustomToken?key=" + FIREBASE_WEB_API_KEY;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(firebaseUri))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            String finalFirebaseIdToken = new JSONObject(response.body()).getString("idToken");
            showStatus("로그인 성공!");
            Platform.runLater(() -> guiService.handleLoginSuccess(finalFirebaseIdToken));
        } else {
            throw new IOException("Firebase REST API sign-in failed: " + response.body());
        }
        
    }
	
    /**
     * 서버 API를 호출하여 Firebase Custom Token을 받아오는 메소드
     * @param googleIdToken
     * @return
     * @throws Exception
     */
    private String getFirebaseCustomToken(String googleIdToken) throws Exception {
        showStatus("서버에 Firebase 토큰을 요청합니다...");
        String requestBody = new JSONObject(Map.of("idToken", googleIdToken)).toString();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/firebase/signin"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return new JSONObject(response.body()).getString("customToken");
        } else {
            throw new IOException("Failed to get custom token from server: " + response.body());
        }
    }


	private void showStatus(String message) {
		Platform.runLater(() -> statusLabel.setText(message));
	}

	/**
	 * 로컬 호스트 동적 등록
	 * 
	 * @return 사용 가능한 포트를 OS에 임시로 할당받고 즉시 닫아서 반환
	 * @throws IOException
	 */
    private int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
