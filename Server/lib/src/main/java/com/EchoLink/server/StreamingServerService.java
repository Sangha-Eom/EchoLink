package com.EchoLink.server;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;

import com.EchoLink.server.config.ServerConfig;
import com.EchoLink.server.handler.ClientHandler;

/**
 * 스트리밍 서버 시작(main) 클래스
 * 
 * Server
 * 1) 애플리케이션 시작 시 인증 웹서버로 데스크탑과 모바일 인증
 * 2) 인증 웹서버로 연결 될 때까지 Poll
 * 3) 스트리밍 서버 시작 및 클라이언트 조종.
 * 4) 이후 인증서버에서 주기적으로 온라인인지 확인.
 * @author ESH
 */
@Service
public class StreamingServerService {

    private final ServerConfig config;	// 서버 기본 설정(application.yml)
    private String jwtToken;			// jwt 인증 토큰
    private ExecutorService clientHandlerPool;	// 클라이언트 입력 쓰레드
    private ServerSocket serverSocket;			// 서버 Socket
    private ScheduledExecutorService heartbeatScheduler;	// 상태 확인 객체
    private volatile boolean running = true;				// 어플 실행 상태

    /**
     * 생성자
     * 서버 기본 설정 로드
     */
    public StreamingServerService() {
        this.config = ServerConfig.load();
    }
    
    
    /**
     * 로그인 성공 후 FxApplication이 메소드 호출
     * 스트리밍 서버의 모든 시작 로직을 담당.
     * @param token 로그인 시 발급받은 JWT
     */
    public void startServer(String token) {
        new Thread(() -> {
            try {
                // 1. 데스크톱 앱 인증 및 JWT 획득
                System.out.println("[EchoLink] 데스크톱 인증을 시작합니다...");
                this.jwtToken = token;
                System.out.println("[EchoLink] 인증 성공! JWT를 발급받았습니다.");

                // 2. 인증 성공 후 Heartbeat 스케줄러 시작
                startHeartbeat();

                // 3. 스트리밍 서버 소켓 열고 클라이언트 연결 대기
                startClientAcceptLoop();

            } catch (Exception e) {
                System.err.println("[EchoLink] 서버 시작 중 치명적인 오류 발생: " + e.getMessage());
                // 실제 앱에서는 이 부분에서 사용자에게 GUI로 오류를 알려야 합니다.
                e.printStackTrace();
            }
        }).start();
    }

    
    /**
     * Spring이 종료되기 직전 이 메소드를 호출 (@PreDestroy)
     * 모든 리소스를 안전하게 종료합니다.
     */
    @PreDestroy
    public void stopServer() {
        running = false;
        System.out.println("[EchoLink] 서버를 종료합니다...");
        if (heartbeatScheduler != null) {
            heartbeatScheduler.shutdownNow();
        }
        if (clientHandlerPool != null) {
            clientHandlerPool.shutdownNow();
        }
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {}
        }
        System.out.println("[EchoLink] 모든 서비스가 종료되었습니다.");
    }

    
    /**
     * 스트리밍 서버 소켓 열고 클라이언트 연결 대기
     * 
     * @throws IOException
     */
    private void startClientAcceptLoop() throws IOException {
    	
    	// 스트리밍 쓰레드
        clientHandlerPool = Executors.newFixedThreadPool(config.getClientThreadPoolSize());
        serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new InetSocketAddress(config.getLoginPort()), config.getServerBacklog());
        System.out.println("[EchoLink] 스트리밍 서버가 포트 " + config.getLoginPort() + "에서 연결을 기다립니다.");
        
        // 스트리밍 쓰레드 시작 -> ClientHandler에게 인계
        while (running) {
            try {
                Socket client = serverSocket.accept();
                System.out.println("[EchoLink] 클라이언트 연결됨: " + client.getInetAddress().getHostAddress());
                clientHandlerPool.submit(new ClientHandler(client, jwtToken, config));
            } catch (IOException e) {
                if (running) {
                    System.err.println("클라이언트 연결 수락 중 오류 발생: " + e.getMessage());
                }
            }
        }
        
    }
    
	/**
	 * 주기적(1분)으로 인증 서버에 상태를 알리는 Heartbeat 스케줄러 메소드
	 */
    private void startHeartbeat() {
         heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
        Runnable heartbeatTask = () -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                String deviceName = InetAddress.getLocalHost().getHostName();
                String requestBody = new JSONObject().put("deviceName", deviceName).toString();	// PC 이름을 기기 이름으로 사용

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(config.getAuthServerUrl() + "/api/devices/heartbeat"))
                        .header("Authorization", "Bearer " + this.jwtToken)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                      .thenAccept(response -> {
                          if (response.statusCode() != 200) {
                              System.err.println("[Heartbeat] 상태 업데이트 실패: " + response.statusCode());
                          }
                      });
            } catch (Exception e) {
                System.err.println("[Heartbeat] 오류: " + e.getMessage());
            }
        };
        // 1분마다 Heartbeat 전송 (서버 부하를 줄이기 위해 폴링 주기보다 길게 설정)
        heartbeatScheduler.scheduleAtFixedRate(heartbeatTask, 0, 1, TimeUnit.MINUTES);
        System.out.println("[EchoLink] Heartbeat 서비스를 시작합니다.");
    }

}