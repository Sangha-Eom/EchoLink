package com.EchoLink.server.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * EchoLink server를 위한 중앙 집중식 구성을 위한 유틸리티 클래스.
 * 
 * 값들 불러오기 (우선 순위부터):
 * 1) 시스템 속성 (e.g., -Decholink.loginPort=20805)
 * 2) 환경변수 (e.g., ECHOLINK_LOGIN_PORT=20805)
 * 3) properties 파일 (./echolink-server.properties)
 * 4) 하드코딩된 기본값
 */
public final class ServerConfig {

    private static final String DEFAULT_PROPERTIES_FILE = "echolink-server.properties";

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
    private ServerConfig(Properties p) {
        this.loginPort = intProp(p, "echolink.loginPort", 20805);
        this.authServerUrl = strProp(p, "echolink.authServerUrl", "http://localhost:8080");
        this.authPollIntervalMs = intProp(p, "echolink.authPollIntervalMs", 5000);
        this.authMaxAttempts = intProp(p, "echolink.authMaxAttempts", 120); // ~10 minutes
        this.serverBacklog = intProp(p, "echolink.serverBacklog", 50);
        this.clientThreadPoolSize = intProp(p, "echolink.clientThreadPoolSize", 16);
    }
    
    /**
     * ServerConfig.java 불러오기
     * @return
     */
    public static ServerConfig load() {
        return new ServerConfig(loadProps());
    }
    
    /**
     * properties 파일로 부터 값들 적용
     * @return properties 파일
     */
    private static Properties loadProps() {
        Properties p = new Properties();
        // 3) Properties file 경로
        Path propsPath = Path.of(DEFAULT_PROPERTIES_FILE);
        if (Files.exists(propsPath)) {
            try (FileInputStream fis = new FileInputStream(propsPath.toFile())) {
                p.load(fis);
            } catch (IOException ignored) {}
        }
        
        // 2) 환경 변수 불러오기
        putIfEnvPresent(p, "echolink.loginPort", "ECHOLINK_LOGIN_PORT");
        putIfEnvPresent(p, "echolink.authServerUrl", "ECHOLINK_AUTH_SERVER_URL");
        putIfEnvPresent(p, "echolink.authPollIntervalMs", "ECHOLINK_AUTH_POLL_INTERVAL_MS");
        putIfEnvPresent(p, "echolink.authMaxAttempts", "ECHOLINK_AUTH_MAX_ATTEMPTS");
        putIfEnvPresent(p, "echolink.serverBacklog", "ECHOLINK_SERVER_BACKLOG");
        putIfEnvPresent(p, "echolink.clientThreadPoolSize", "ECHOLINK_CLIENT_THREADPOOL_SIZE");
        
        // 1) 시스템 속성은 자동으로 재정의 됨.(via. Properties.getProperty(key, System.getProperty(key, ...)))

        return p;
    }
    
    /**
     * 환경 변수 불러오기
     * 
     * @param p			properties 파일
     * @param key		properties 키값
     * @param envKey	properties 키의 값
     */
    private static void putIfEnvPresent(Properties p, String key, String envKey) {
        String v = System.getenv(envKey);
        if (v != null && !v.isBlank()) 
        	p.setProperty(key, v);
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
    private static int intProp(Properties p, String k, int def) {
        String sys = System.getProperty(k);	// 키의 값
        String v = (sys != null) ? sys : p.getProperty(k);
        
        if (v == null) 
        	return def;
        
        try { 
        	return Integer.parseInt(v.trim()); 
        }
        catch (Exception e) {
        	return def; 
        }
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
    private static String strProp(Properties p, String k, String def) {
        String sys = System.getProperty(k);
        String v = (sys != null) ? sys : p.getProperty(k);
        
        return (v == null || v.isBlank()) ? def : v.trim();
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

    @Override
    public String toString() {
        return "ServerConfig{" +
                "loginPort=" + loginPort +
                ", authServerUrl='" + authServerUrl + '\'' +
                ", authPollIntervalMs=" + authPollIntervalMs +
                ", authMaxAttempts=" + authMaxAttempts +
                ", serverBacklog=" + serverBacklog +
                ", clientThreadPoolSize=" + clientThreadPoolSize +
                '}';
    }
}