package com.EchoLink.auth_server;

import com.EchoLink.server.StreamingServerService;
import com.EchoLink.server.gui.MainController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

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

    @Override
    public void init() {
        // JavaFX 스레드에서 Spring Boot 애플리케이션을 초기화합니다.
        springContext = new SpringApplicationBuilder(AuthServerApplication.class).run();
    }

    @Override
    public void start(Stage stage) throws Exception {
        // ✅ Spring 컨텍스트에서 FXML 로더와 컨트롤러를 가져옵니다.
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/EchoLink/server/gui/MainView.fxml"));
        fxmlLoader.setControllerFactory(springContext::getBean); // Spring이 컨트롤러를 관리하도록 설정
        Parent root = fxmlLoader.load();

        stage.setTitle("EchoLink Server");
        stage.setScene(new Scene(root));
        stage.show();

        // ✅ Spring 컨텍스트에서 StreamingServerService와 MainController 인스턴스를 가져옵니다.
        StreamingServerService serverService = springContext.getBean(StreamingServerService.class);
        MainController mainController = fxmlLoader.getController(); // 로더로부터 컨트롤러 인스턴스를 직접 얻습니다.

        // ✅ 서버 서비스에 GUI 컨트롤러를 연결합니다.
        serverService.setGuiController(mainController);
    }

    @Override
    public void stop() {
        // ✅ 애플리케이션 종료 시 Spring 컨텍스트를 닫습니다.
        springContext.close();
        Platform.exit();
    }
}