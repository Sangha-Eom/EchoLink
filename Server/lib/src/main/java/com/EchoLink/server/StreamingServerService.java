package com.EchoLink.server;

import jakarta.annotation.PreDestroy;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
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

import com.EchoLink.server.config.ServerConfig;
import com.EchoLink.server.handler.ClientHandler;
import com.EchoLink.server.handler.StreamSessionManager;

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

    private StreamSessionManager streamSessionManager; // 현재 활성화된 스트리밍 세션을 관리
    
    /**
     * 생성자
     * 서버 기본 설정 로드
     * @param config Spring이 자동으로 ServerConfig Bean을 주입합니다.
     */
    @Autowired
    public StreamingServerService(ServerConfig config) {
        this.config = config;
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
     * GUI에서 사용자가 선택한 설정으로 스트리밍 세션을 시작합니다.
     * @param clientIp 연결할 클라이언트 IP
     * @param resolution "가로x세로" 형태의 해상도 문자열
     * @param bitrate bps 단위의 비트레이트
     * @param fps 초당 프레임 수
     */
    public void startStreamingSession(String clientIp, String resolution, int bitrate, int fps) {
        if (streamSessionManager != null) {
            System.err.println("이미 스트리밍 세션이 진행 중입니다.");
            return;
        }

        try {
            String[] dimensions = resolution.split("x");
            int width = Integer.parseInt(dimensions[0]);
            int height = Integer.parseInt(dimensions[1]);
            
            // TODO: 실제 클라이언트의 UDP 포트를 받아와야 함. 
            // 우선 임의의 포트(예: 9999) 사용
            int clientUdpPort = 9999; 

            // StreamSessionManager를 생성하고 세션을 시작.
            streamSessionManager = new StreamSessionManager(clientIp, fps, bitrate * 1_000_000, width, height, clientUdpPort); // bps 단위로 변환
            streamSessionManager.startSession();
            
            System.out.println("GUI에 의해 스트리밍 세션이 시작되었습니다: " + clientIp);

        } catch (Exception e) {
            e.printStackTrace();
            // TODO: GUI에 오류 메시지를 표시하는 로직 추가 필요
        }
    }

    /**
     * 현재 진행 중인 스트리밍 세션을 중지합니다.
     */
    public void stopStreamingSession() {
        if (streamSessionManager != null) {
            streamSessionManager.stopSession();
            streamSessionManager = null; // 세션 정리
            System.out.println("GUI에 의해 스트리밍 세션이 중지되었습니다.");
        }
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
