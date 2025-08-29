package com.EchoLink.auth_server;


import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.EchoLink.server.StreamingServerService;
import com.EchoLink.server.gui.LoginController;

/**
 * GUI 생성 및 화면 출력 클래스
 * 
 * 실제 어플리케이션 창
 * Spring Boot 실행 후 Spring 서비스 연결
 * 
 * 스트리밍: StreamingServerService.java
 * GUI: MainController.java
 */
public class FxApplication extends Application {

	private ConfigurableApplicationContext springContext;
	private Stage stage; // Stage를 멤버 변수로 저장하여 화면 전환에 사용

	// 로그인 성공 후 JWT 설정 파일을 저장할 경로
	private static final Path TOKEN_PATH = Paths.get(System.getProperty("user.home"), ".echolink", "session.token");

	@Override
	public void init() {
		// Spring Boot 어플 초기화
		springContext = new SpringApplicationBuilder(AuthServerApplication.class).run();
	}

	@Override
	public void start(Stage stage) throws Exception {
		this.stage = stage;
		// 앱 시작 시 저장된 토큰이 있는지 확인
		String savedToken = loadToken();

		if (savedToken != null && !savedToken.isBlank()) {
			// 토큰이 있으면 바로 메인 화면으로 이동
			onLoginSuccess(savedToken);
		}
		else {
			// 토큰이 없으면 로그인 화면 표시
			showLoginScene();
		}
	}

	/**
	 * 로그인 화면을 로드하고 표시하는 메소드
	 */
	private void showLoginScene() throws IOException {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/EchoLink/server/gui/LoginView.fxml"));
		fxmlLoader.setControllerFactory(springContext::getBean);
		Parent root = fxmlLoader.load();

		LoginController loginController = fxmlLoader.getController();
		loginController.setFxApplication(this); // 컨트롤러가 FxApplication을 참조하도록 설정

		stage.setTitle("EchoLink - 로그인");
		stage.setScene(new Scene(root));
		stage.show();
	}

	/**
	 * 메인 화면(연결 대기)을 로드하고 표시하는 메소드
	 */
	private void showMainScene() throws IOException {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/EchoLink/server/gui/MainView.fxml"));
		fxmlLoader.setControllerFactory(springContext::getBean);
		Parent root = fxmlLoader.load();

		// TODO: MainController 설정
		// MainController mainController = fxmlLoader.getController();
		// serverService.setGuiController(mainController); // MainController UI 업데이트 로직은 제거했으므로 주석 처리

		stage.setTitle("EchoLink Server - 연결 대기 중");
		stage.setScene(new Scene(root));
		stage.show();
	}


	/**
	 * LoginController가 로그인 성공 시 호출하는 콜백 메소드
	 * @param token 서버로부터 발급받은 JWT
	 */
	public void onLoginSuccess(String token) {
		// 1. 토큰을 파일에 저장
		saveToken(token);

		// 2. 스트리밍 서비스 시작
		// Spirng 컨텍스트에서 StreamingServerService를 통해 토큰 가져오기
		StreamingServerService serverService = springContext.getBean(StreamingServerService.class);
		serverService.startServer(token); // 토큰을 전달하여 서비스 시작

		// 3. UI를 메인 화면으로 전환
		Platform.runLater(() -> {
			try {
				showMainScene();
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

	// --- 토큰 저장 및 로드 헬퍼 메소드 ---
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


	@Override
	public void stop() {
		springContext.close();
		Platform.exit();
	}
}