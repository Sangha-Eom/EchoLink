package com.EchoLink.server.gui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import com.EchoLink.server.StreamingServerService;


/**
 * GUI 조작 클래스
 * 
 * FXML의 디자인, 동작 설정(버튼 클릭, 텍스트 변경 등)
 * Route: MainView.fxml
 */
@Component
public class MainController {

	// FXML에서 추가한 UI 요소들을 멤버 변수로 선언
	@FXML private Label statusLabel;
	@FXML private TextArea logTextArea; // 로그 출력
	@FXML private Button logoutButton;

	@Autowired
	private StreamingServerService streamingServerService; // Spring의존성 주입(DI)

	private GuiService guiService;
	private String jwtToken; // 로그인 시 GuiService로부터 받아야 함

	// GuiService로부터 인스턴스를 전달받기 위한 메소드
	public void setGuiService(GuiService guiService) {
		this.guiService = guiService;
	}

	/**
	 * GuiService로부터 JWT 토큰을 전달받는 메소드 (GuiService에서 호출)
	 * @param token
	 */
	public void setJwtToken(String token) {
		this.jwtToken = token;
		
		// 로그인 성공 시 토큰을 받으면 초기 상태 메시지 설정
		updateStatus("서버 실행 중... 모바일 클라이언트의 연결을 기다립니다.");
	}

	/**
	 * FXML 파일 로드 후 자동 호출되는 초기화 메소드
	 */
	@FXML
	public void initialize() {
		// TODO: 추후 추가 사항
	}

	/**
	 * 로그아웃 버튼 클릭 시 호출될 메소드
	 */
	@FXML
	private void handleLogout() {
		if (guiService != null) {
			guiService.handleLogout();
		} else {
			System.err.println("오류: GuiService가 MainController에 주입되지 않았습니다.");
		}
	}
	

    /**
     *  다른 클래스에서 상태 메시지를 업데이트할 수 있도록 public 메소드 제공
     * @param message
     */
    public void updateStatus(String message) {
        Platform.runLater(() -> statusLabel.setText(message));
    }

    /**
     *  다른 클래스에서 로그를 추가할 수 있도록 public 메소드 제공
     * @param logMessage
     */
    public void addLog(String logMessage) {
        Platform.runLater(() -> logTextArea.appendText(logMessage + "\n"));
    }

}
