package com.EchoLink.auth_server;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import com.EchoLink.server.gui.GuiService;

/**
 * GUI 생성 및 화면 출력 클래스
 * 
 * 실제 어플리케이션 창
 * Spring Boot 실행 후 Spring 서비스 연결
 * 로그인 성공 후 받은 Firebase ID 토큰을 StreamingServerService에 전달
 * 
 * 스트리밍: StreamingServerService.java
 * GUI: MainController.java
 */
public class FxApplication extends Application {

	private ConfigurableApplicationContext springContext;

	@Override
	public void init() {
		// Spring Boot 어플 초기화
		springContext = new SpringApplicationBuilder(AuthServerApplication.class)
				.headless(false)
				.run();
	}
	
	/**
	 * 애플리케이션 초기화 로직 실행
	 * GuiService.java
	 */
    @Override
    public void start(Stage stage) throws Exception {

        GuiService guiService = springContext.getBean(GuiService.class);
        guiService.setStage(stage);
        guiService.initializeApplication();
    }

    @Override
    public void stop() {
        springContext.close();
        Platform.exit();
    }
    
}
