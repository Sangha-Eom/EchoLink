package com.EchoLink.server.gui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.springframework.stereotype.Component;

/**
 * GUI 조작 클래스
 * 
 * FXML의 디자인, 동작 설정(버튼 클릭, 텍스트 변경 등)
 * Route: MainView.fxml
 */
@Component
public class MainController {

    @FXML
    private Label statusLabel;

    // TODO: GuiService를 주입받아 로그아웃 등의 기능 추가
    // private GuiService guiService;
    // public void setGuiService(GuiService guiService) { this.guiService = guiService; }

    @FXML
    public void initialize() {
        // 초기화 로직 (예: 서버 상태 표시)
        statusLabel.setText("서버가 실행 중입니다. 클라이언트의 연결을 기다립니다.");
    }
}
