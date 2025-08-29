package com.EchoLink.server.gui;

import com.EchoLink.auth_server.FxApplication;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class LoginController {

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
            .whenComplete((response, error) -> {
                Platform.runLater(() -> {
                    if (error != null) {
                        showStatus("서버 통신 오류: " + error.getMessage());
                    } else {
                        handleResponse(url, response.statusCode(), response.body());
                    }
                    setUiLoading(false); // UI 다시 활성화
                });
            });
    }

    private void handleResponse(String url, int statusCode, String body) {
         if (url.endsWith("/login")) {
            if (statusCode == 200) {
                JSONObject json = new JSONObject(body);
                fxApplication.onLoginSuccess(json.getString("token")); // 로그인 성공 처리
            } else {
                showStatus("로그인 실패: " + body);
            }
        } else if (url.endsWith("/register")) {
            if (statusCode == 200) {
                 showStatus("회원가입 성공! 이제 로그인해주세요.");
            } else {
                showStatus("회원가입 실패: " + body);
            }
        }
    }

    private void showStatus(String message) {
        statusLabel.setText(message);
    }

    private void setUiLoading(boolean isLoading) {
        loginButton.setDisable(isLoading);
        registerButton.setDisable(isLoading);
    }
}