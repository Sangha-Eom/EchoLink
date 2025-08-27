package com.EchoLink.server.config;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;
import java.util.Objects;

/**
 * EchoLink server를 위한 중앙 집중식 구성을 위한 유틸리티 클래스.
 * src/main/resources/application.yml 파일에서 설정을 로드.
 * 
 * 값들 불러오기 (우선 순위부터):
 * 1) 시스템 속성 (e.g., -Decholink.loginPort=20805)
 * 2) 환경변수 (e.g., ECHOLINK_LOGIN_PORT=20805)
 * 3) properties 파일 (./echolink-server.properties)
 * 4) 하드코딩된 기본값
 */
public final class ServerConfig {

    private final String jwtSecret; 		// JWT Secret Key

    private final int loginPort;			// 로그인 포트
    private final String authServerUrl;		// 인증 서버 URL
    private final int authPollIntervalMs;	// 인증 폴링 간격(인증 시도 시간)
    private final int authMaxAttempts;		// 인증 최대 횟수
    private final int serverBacklog;		// 서버 백 로그
    private final int clientThreadPoolSize;	// 쓰레드 풀 사이즈

    
    /**
     * 생성자
     * 객체 생성 방지를 위한 private
     * @param p
     */
    @SuppressWarnings("unchecked")
    private ServerConfig(Map<String, Object> config) {
    	// YAML의 계층 구조를 따라 값 추출
        Map<String, Object> echolinkConfig = (Map<String, Object>) config.get("echolink");
        Map<String, Object> authConfig = (Map<String, Object>) echolinkConfig.get("auth");
        Map<String, Object> serverConfig = (Map<String, Object>) echolinkConfig.get("server");
        Map<String, Object> jwtConfig = (Map<String, Object>) config.get("jwt");
        
        this.loginPort = intProp(serverConfig, "loginPort", 20805);
        this.authServerUrl = strProp(authConfig, "serverUrl", "http://localhost:8080");
        this.authPollIntervalMs = intProp(authConfig, "pollIntervalMs", 5000);
        this.authMaxAttempts = intProp(authConfig, "maxAttempts", 120);
        this.serverBacklog = intProp(serverConfig, "backlog", 50);
        this.clientThreadPoolSize = intProp(serverConfig, "clientThreadPoolSize", 16);
        this.jwtSecret = strProp(jwtConfig, "secret", null); // Secret은 기본값 없음
        
        Objects.requireNonNull(this.jwtSecret, "JWT secret key in application.yml cannot be null.");
    }
    
    
    /**
     * ServerConfig.java 객체 생성
     * @return
     */
    public static ServerConfig load() {
    	
    	Yaml yaml = new Yaml();
        // ClassLoader를 통해 resources 폴더의 application.yml 파일을 InputStream으로 읽어옴
        InputStream inputStream = ServerConfig.class
                .getClassLoader()
                .getResourceAsStream("application.yml");
        
        Objects.requireNonNull(inputStream, "Could not find application.yml in resources.");

        Map<String, Object> config = yaml.load(inputStream);
        
        return new ServerConfig(config);
    }

    
    /**
     * int값 적용
     * 
     * @param p properties 파일(경로)
     * @param k key값
     * @param def 기본값
     * @return key의 값 불러오기
     * 			실패시 디폴트값 적용.
     */
    private int intProp(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        return defaultValue;
    }

    /**
     * String값 적용
     * 
     * @param p properties 파일(경로)
     * @param k key값
     * @param def 기본값
     * @return key의 값 불러오기
     * 			실패시 디폴트값 적용.
     */
    private String strProp(Map<String, Object> map, String key, String defaultValue) {
        return (String) map.getOrDefault(key, defaultValue);
    }
    
    
    
    /**
     * Getter 메소드
     * @return 로그인 포트
     */
    public int getLoginPort() { 
    	return loginPort; 
    }
    /**
     * Getter 메소드
     * @return 인증 서버 URL
     */
    public String getAuthServerUrl() { 
    	return authServerUrl; 
    }
    /**
     * Getter 메소드
     * @return 인증 시간 간격
     */
    public int getAuthPollIntervalMs() { 
    	return authPollIntervalMs; 
    }
    /**
     * Getter 메소드
     * @return 인증 시간 간격
     */
    public int getAuthMaxAttempts() { 
    	return authMaxAttempts;
    }
    /**
     * Getter 메소드
     * @return 서버 백 로그
     */
    public int getServerBacklog() { 
    	return serverBacklog;
    }
    /**
     * Getter 메소드
     * @return 클라이언트 쓰레드 풀 사이즈
     */
    public int getClientThreadPoolSize() { 
    	return clientThreadPoolSize; 
    }
    
    /**
     * Getter 메소드
     * @return jwtSecret
     */
    public String getJwtSecret() { 
    	return jwtSecret; 
    }

    @Override
    public String toString() {
        return "ServerConfig{" +
                "loginPort=" + loginPort +
                ", authServerUrl='" + authServerUrl + '\'' +
                ", authPollIntervalMs=" + authPollIntervalMs +
                ", authMaxAttempts=" + authMaxAttempts +
                ", serverBacklog=" + serverBacklog +
                ", clientThreadPoolSize=" + clientThreadPoolSize +
                ", jwtSecret='[PROTECTED]'" + // 로그에 Secret이 노출되지 않도록 수정
                '}';
    }
}