package com.EchoLink.server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Server 설정값 설정 클래스
 * Spring의 컴포넌트 등록(Bean 사용
 */
@Configuration
public class AppConfig {

    @Bean
    public ServerConfig serverConfig(
            @Value("${jwt.secret}") String jwtSecret,
            @Value("${echolink.server.loginPort}") int loginPort,
            @Value("${echolink.auth.serverUrl}") String authServerUrl,
            @Value("${echolink.auth.pollIntervalMs}") int authPollIntervalMs,
            @Value("${echolink.auth.maxAttempts}") int authMaxAttempts,
            @Value("${echolink.server.backlog}") int serverBacklog,
            @Value("${echolink.server.clientThreadPoolSize}") int clientThreadPoolSize) {
        return new ServerConfig(jwtSecret, loginPort, authServerUrl, authPollIntervalMs, authMaxAttempts, serverBacklog, clientThreadPoolSize);
    }
}
