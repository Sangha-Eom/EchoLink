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

    // 로그아웃 처리 메소드
    public void handleLogout() {
        // 1. 실행 중인 스트리밍 서버 및 Heartbeat 서비스 중지
        streamingServerService.stopServer();

        // 2. 저장된 세션 토큰 파일 삭제
        deleteToken();

        // 3. UI를 로그인 화면으로 전환
        Platform.runLater(() -> {
            try {
                showLoginScene();
            } catch (IOException e) {
                System.err.println("로그인 화면으로 돌아가는 중 오류 발생");
                e.printStackTrace();
            }
        });
    }
    
    // 토큰 파일을 삭제하는 private 메소드
    private void deleteToken() {
        try {
            if (Files.exists(TOKEN_PATH)) {
                Files.delete(TOKEN_PATH);
                System.out.println("세션 토큰이 삭제되었습니다.");
            }
        } catch (IOException e) {
            System.err.println("토큰 삭제에 실패했습니다: " + e.getMessage());
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
    	FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/loginView.fxml"));
        fxmlLoader.setControllerFactory(springContext::getBean);
        Parent root = fxmlLoader.load();

        LoginController loginController = fxmlLoader.getController();
        loginController.setGuiService(this); // GuiService 참조 설정

        stage.setTitle("EchoLink - 로그인");
        stage.setScene(new Scene(root));
        stage.show();
    }

    private void showMainScene() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
        fxmlLoader.setControllerFactory(springContext::getBean);
        Parent root = fxmlLoader.load();

        // MainController를 가져와 GuiService 참조 설정
        MainController mainController = fxmlLoader.getController();
        mainController.setGuiService(this); 	// GuiService 참조 전달
        mainController.setJwtToken(loadToken()); // JWT 토큰 전달

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
