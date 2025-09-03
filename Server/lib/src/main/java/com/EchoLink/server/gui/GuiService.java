package com.EchoLink.server.gui;

import com.EchoLink.server.StreamingServerService;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 화면 전환, 토큰 관리, 스트리밍 서비스 시작/종료
 */
@Service
public class GuiService {

    private final ConfigurableApplicationContext springContext;
    private final StreamingServerService streamingServerService;
    private Stage stage;

    private static final Path TOKEN_PATH = Paths.get(System.getProperty("user.home"), ".echolink", "session.token");

    public GuiService(ConfigurableApplicationContext springContext, StreamingServerService streamingServerService) {
        this.springContext = springContext;
        this.streamingServerService = streamingServerService;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void initializeApplication() throws IOException {
        String savedToken = loadToken();
        if (savedToken != null && !savedToken.isBlank()) {
            // 토큰이 있으면 바로 메인 화면으로 이동 및 서비스 시작
            handleLoginSuccess(savedToken);
        } else {
            // 토큰이 없으면 로그인 화면 표시
            showLoginScene();
        }
    }

    public void handleLoginSuccess(String idToken) {
        saveToken(idToken);
        streamingServerService.startServer(idToken); // 스트리밍 서비스 시작

        // UI를 메인 화면으로 전환
        Platform.runLater(() -> {
            try {
                showMainScene();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void showLoginScene() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/EchoLink/server/gui/LoginView.fxml"));
        fxmlLoader.setControllerFactory(springContext::getBean);
        Parent root = fxmlLoader.load();

        LoginController loginController = fxmlLoader.getController();
        loginController.setGuiService(this); // GuiService 참조 설정

        stage.setTitle("EchoLink - 로그인");
        stage.setScene(new Scene(root));
        stage.show();
    }

    private void showMainScene() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/EchoLink/server/gui/MainView.fxml"));
        fxmlLoader.setControllerFactory(springContext::getBean);
        Parent root = fxmlLoader.load();

        // MainController 설정 (필요 시)
        // MainController mainController = fxmlLoader.getController();
        // mainController.setGuiService(this);

        stage.setTitle("EchoLink Server - 연결 대기 중");
        stage.setScene(new Scene(root));
        stage.show();
    }

    private void saveToken(String token) {
        try {
            Files.createDirectories(TOKEN_PATH.getParent());
            Files.writeString(TOKEN_PATH, token);
        } catch (IOException e) {
            System.err.println("토큰 저장에 실패했습니다: " + e.getMessage());
        }
    }

    private String loadToken() {
        try {
            if (Files.exists(TOKEN_PATH)) {
                return Files.readString(TOKEN_PATH);
            }
        } catch (IOException e) {
            System.err.println("토큰 로드에 실패했습니다: " + e.getMessage());
        }
        return null;
    }
}
