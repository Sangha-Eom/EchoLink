package com.EchoLink.server.gui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.net.URI;

/**
 * GUI 조작 클래스
 * 
 * FXML의 디자인, 동작 설정(버튼 클릭, 텍스트 변경 등)
 * Route: MainView.fxml
 */
@Component
public class MainController {

    @FXML private Label statusLabel;
    @FXML private VBox authBox;
    @FXML private Label deviceCodeLabel;
    @FXML private Label infoLabel;
    
    
    private String authUrl;
    
    
    /**
     * 
     * @param authUrl
     */
    public void setAuthUrl(String authUrl) {
        this.authUrl = authUrl + "/link";
    }

    /**
     *  인증 페이지 열기 버튼 클릭 시 호출
     */
    @FXML
    private void openAuthPage() {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(this.authUrl));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *  외부(StreamingServerService)에서 UI 상태를 변경하기 위한 메소드들
     * @param code 인증 코드
     */
    public void showAuthCode(String code) {
        Platform.runLater(() -> {
            statusLabel.setText("사용자 인증 대기 중...");
            deviceCodeLabel.setText(code);
            authBox.setVisible(true);
        });
    }
    
    /**
     * 연결 상태 
     */
    public void showConnecting() {
        Platform.runLater(() -> {
            authBox.setVisible(false);
            statusLabel.setText("인증 완료! 연결 대기 중...");
            infoLabel.setText("모바일 앱에서 이 PC를 선택하여 연결하세요.");
            infoLabel.setVisible(true);
        });
    }

    /**
     * 오류 발생 메세지
     * @param message 오류 발생
     */
    public void showError(String message) {
        Platform.runLater(() -> {
            statusLabel.setText("오류 발생");
            infoLabel.setText(message);
            infoLabel.setVisible(true);
            authBox.setVisible(false);
        });
    }
    
}