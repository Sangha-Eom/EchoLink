package com.EchoLink.auth_server;

import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Spring Boot의 설정 파일 역할
 * 실행은 FxApplication 클래스로 이양
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@ComponentScan(basePackages = {"com.EchoLink.auth_server", "com.EchoLink.server"})
public class AuthServerApplication {

    public static void main(String[] args) {
        // Spring Boot 앱을 직접 실행하는 대신, FxApplication을 실행
        Application.launch(FxApplication.class, args);
    }
}
