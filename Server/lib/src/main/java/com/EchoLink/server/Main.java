package com.EchoLink.server;

import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.net.InetAddress;
import java.io.IOException;

import com.EchoLink.server.handler.ClientHandler;
import com.EchoLink.server.config.ServerConfig;

/**
 * Main 클래스
 * 
 * 1) 애플리케이션 시작 시 인증 웹서버로 데스크탑과 모바일 인증
 * 2) 인증 웹서버로 연결 될 때까지 Poll
 * 3) 스트리밍 서버 시작 및 클라이언트 조종.
 * 4) 이후 인증서버에서 주기적으로 온라인인지 확인.
 * @author ESH
 */
public class Main {

	public static void main(String[] args) {

		// 서버 설정 로드
		final ServerConfig config = ServerConfig.load();
		System.out.println("[EchoLink] Loaded config: " + config);

		// 서버 인증
		try {
			String jwtToken = performAuthHandshake(config);
			System.out.println("[EchoLink] Auth OK. Starting streaming server...");
			startHeartbeat(config, jwtToken);	// 기기 온라인 상태 확인
			startStreamingServer(config, jwtToken);	// 스트리밍 시작
		} catch (Exception e) {
			System.err.println("[EchoLink] Fatal error during startup: " + e.getMessage());
			e.printStackTrace();
		}

	}


	/**
	 * 서버 인증
	 * 
	 * @param config 설정값
	 * @return JWT 인증 토큰
	 * @throws Exception
	 */
	public static String performAuthHandshake(ServerConfig config) throws Exception {

		// 1. 데스크톱 서버를 인증 서버에 등록하고 고유 코드 받기
		final HttpClient client = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(5))
				.build();

		System.out.println("[EchoLink] Registering device with auth server...");
		HttpRequest register = HttpRequest.newBuilder()
				.uri(URI.create(config.getAuthServerUrl() + "/api/devices/register"))
				.timeout(Duration.ofSeconds(5))
				.POST(HttpRequest.BodyPublishers.noBody())
				.build();

		HttpResponse<String> response = client.send(register, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() != 200) {
			throw new IllegalStateException("Auth server returned " + response.statusCode() + " on register");
		}
		String body = response.body();
		if (body == null || body.isBlank()) {
			throw new IllegalStateException("Empty response from auth server");
		}
		String deviceCode = new JSONObject(body).getString("deviceCode");

		System.out.println("======================================================");
		System.out.println("Open " + config.getAuthServerUrl() + "/link in a browser");
		System.out.println("Enter the code below to link this desktop:");
		System.out.println("CODE: " + deviceCode);
		System.out.println("======================================================");


		// 2. 인증이 완료될 때까지 주기적으로 Polling
		//		제한 횟수까지 받지 못할 시 종료.
		String jwtToken = null;
		int attempts = 0;
		while (jwtToken == null && attempts < config.getAuthMaxAttempts()) {
			attempts++;
			try {
				Thread.sleep(config.getAuthPollIntervalMs());
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
				throw ie;
			}
			System.out.println("[EchoLink] Checking link status... (attempt " + attempts + ")");

			HttpRequest poll = HttpRequest.newBuilder()
					.uri(URI.create(config.getAuthServerUrl() + "/api/devices/poll/" + deviceCode))
					.timeout(Duration.ofSeconds(5))
					.GET()
					.build();

			HttpResponse<String> pollResp = client.send(poll, HttpResponse.BodyHandlers.ofString());
			if (pollResp.statusCode() == 200 && pollResp.body() != null && !pollResp.body().isBlank()) {
				jwtToken = new JSONObject(pollResp.body()).getString("token");
				System.out.println("[EchoLink] Device linked successfully. JWT received.");
			} else {
				System.out.println("[EchoLink] Not linked yet (status " + pollResp.statusCode() + ").");
			}
		}

		if (jwtToken == null) {
			throw new IllegalStateException("Linking timed out. Try again.");
		}
		return jwtToken;

	}

	/**
	 * 주기적(1분)으로 인증 서버에 상태를 알리는 Heartbeat 스케줄러 메소드
	 */
	private static void startHeartbeat(ServerConfig config, String jwtToken) {
		ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

		Runnable heartbeatTask = () -> {
			try {
				HttpClient client = HttpClient.newHttpClient();
				String deviceName = InetAddress.getLocalHost().getHostName(); // PC 이름을 기기 이름으로 사용

				String requestBody = new JSONObject()
						.put("deviceName", deviceName)
						.toString();

				HttpRequest request = HttpRequest.newBuilder()
						.uri(URI.create(config.getAuthServerUrl() + "/api/devices/heartbeat"))
						.header("Authorization", "Bearer " + jwtToken)
						.header("Content-Type", "application/json")
						.POST(HttpRequest.BodyPublishers.ofString(requestBody))
						.build();

				client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
					.thenAccept(response -> {
						if (response.statusCode() == 200) {
							System.out.println("[Heartbeat] Status updated successfully.");
						} 
						else {
							System.err.println("[Heartbeat] Failed to update status: " + response.statusCode());
						}
					});
			} 
			catch (Exception e) {
				System.err.println("[Heartbeat] Error: " + e.getMessage());
			}
		};

		// 1분마다 Heartbeat 전송 (서버 부하를 줄이기 위해 폴링 주기보다 길게 설정)
		scheduler.scheduleAtFixedRate(heartbeatTask, 0, 1, TimeUnit.MINUTES);
		System.out.println("[EchoLink] Heartbeat service started.");
	}


	/**
	 * EchoLink 스트리밍 시작
	 * 
	 * @param config 설정값
	 * @param jwtToken JWT 인증 토큰
	 * @throws IOException
	 */
	private static void startStreamingServer(ServerConfig config, String jwtToken) throws IOException {

		final ServerSocket serverSocket = new ServerSocket();
		serverSocket.setReuseAddress(true);
		serverSocket.bind(new InetSocketAddress(config.getLoginPort()), config.getServerBacklog());
		
		// 스트리밍 쓰레드
		final ExecutorService pool = Executors.newFixedThreadPool(config.getClientThreadPoolSize());
		System.out.println("[EchoLink] Streaming server started on port " + config.getLoginPort());

		// 스트리밍 쓰레드 종료
	    Runtime.getRuntime().addShutdownHook(
	            new Thread(() -> {
	                System.out.println("[EchoLink] Shutting down...");
	                
	                // 종료 시 인증 서버에 오프라인 상태 전송
	                try {
	                    HttpClient client = HttpClient.newHttpClient();
	                    String deviceName = InetAddress.getLocalHost().getHostName();
	                    String requestBody = new JSONObject().put("deviceName", deviceName).toString();

	                    HttpRequest request = HttpRequest.newBuilder()
	                            .uri(URI.create(config.getAuthServerUrl() + "/api/devices/shutdown"))
	                            .header("Authorization", "Bearer " + jwtToken)
	                            .header("Content-Type", "application/json")
	                            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
	                            .build();
	                    
	                    // 동기적으로 요청을 보내고 종료를 기다림
	                    client.send(request, HttpResponse.BodyHandlers.ofString());
	                    System.out.println("[Shutdown] Offline status sent to auth server.");

	                } catch (Exception e) {
	                    System.err.println("[Shutdown] Failed to send offline status: " + e.getMessage());
	                }

	                try {
	                    serverSocket.close();
	                } 
	                catch (IOException ignored) {}
	                
	                pool.shutdownNow();
	            }));
	    
	    
		// 스트리밍 쓰레드 시작 -> ClientHanlder에게 인계
		while (true) {
			Socket client = serverSocket.accept();
			System.out.println("[EchoLink] Client connected: " + client.getInetAddress().getHostAddress());
			pool.submit(new ClientHandler(client, jwtToken, config));
		}

	}
}