package com.EchoLink.server.gui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import org.json.JSONObject;

import com.EchoLink.auth_server.FxApplication;

/**
 * GUI 조작 클래스
 * 
 * FXML의 디자인, 동작 설정(버튼 클릭, 텍스트 변경 등)
 * Route: MainView.fxml
 */
@Component
public class MainController {

	@FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Button registerButton;
    @FXML private Label statusLabel;
    
    private final String apiBaseUrl = "http://localhost:8080/api/auth";
    private final HttpClient client = HttpClient.newHttpClient();
    private FxApplication fxApplication;
    
    // FxApplication에서 이 메소드를 호출하여 상호작용할 수 있도록 함
    public void setFxApplication(FxApplication fxApplication) {
        this.fxApplication = fxApplication;
    }

    @FXML
    private void handleLogin() {
        String email = emailField.getText();
        String password = passwordField.getText();
        if (email.isEmpty() || password.isEmpty()) {
            showStatus("이메일과 비밀번호를 모두 입력해주세요.");
            return;
        }
        sendAuthRequest(apiBaseUrl + "/login", email, password);
    }

    @FXML
    private void handleRegister() {
        String email = emailField.getText();
        String password = passwordField.getText();
        if (email.isEmpty() || password.isEmpty()) {
            showStatus("이메일과 비밀번호를 모두 입력해주세요.");
            return;
        }
        sendAuthRequest(apiBaseUrl + "/register", email, password);
    }

    private void sendAuthRequest(String url, String email, String password) {
        setUiLoading(true); // UI 비활성화
        String requestBody = new JSONObject()
                .put("email", email)
                .put("password", password)
                .toString();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(HttpResponse::body)
            .thenAccept(body -> {
                // 서버 응답 처리
                Platform.runLater(() -> {
                    if (url.endsWith("/login")) {
                        JSONObject json = new JSONObject(body);
                        if (json.has("token")) {
                            // 로그인 성공!
                            fxApplication.onLoginSuccess(json.getString("token"));
                        } else {
                            showStatus("로그인 실패: " + body);
                        }
                    } else {
                        // 회원가입 성공/실패 메시지 표시
                        showStatus(body);
                    }
                    setUiLoading(false); // UI 다시 활성화
                });
            })
            .exceptionally(e -> {
                // 예외 처리
                Platform.runLater(() -> {
                    showStatus("오류 발생: " + e.getMessage());
                    setUiLoading(false);
                });
                return null;
            });
    }

    private void showStatus(String message) {
        statusLabel.setText(message);
    }

    private void setUiLoading(boolean isLoading) {
        loginButton.setDisable(isLoading);
        registerButton.setDisable(isLoading);
    }
}