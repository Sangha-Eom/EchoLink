package com.EchoLink.server;

import com.EchoLink.server.handler.ClientHandler;
import org.json.JSONObject;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * 애플리케이션 시작 시 인증 서버와 통신하여 인증을 먼저 수행하고,
 * 인증 완료 후 클라이언트의 접속을 기다립니다.
 * @author ESH
 */
public class Main {
    
    public static int loginPort = 20805;	// 로그인 포트
    private static final String AUTH_SERVER_URL = "http://localhost:8080"; // 인증 웹 서버 주소
    
    
    public static void main(String[] args) {
        try {
            // 1. 데스크톱 서버를 인증 서버에 등록하고 고유 코드 받기
            System.out.println("인증 서버에 기기를 등록합니다...");
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(AUTH_SERVER_URL + "/api/devices/register"))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String deviceCode = new JSONObject(response.body()).getString("deviceCode");

            System.out.println("======================================================");
            System.out.println("웹 브라우저에서 " + AUTH_SERVER_URL + "/link 로 접속하여");
            System.out.println("아래 코드를 입력해주세요:");
            System.out.println("인증 코드: " + deviceCode);
            System.out.println("======================================================");

            // 2. 인증이 완료될 때까지 주기적으로(Polling) 확인
            String jwtToken = null;
            while (jwtToken == null) {
                Thread.sleep(5000); // 5초 대기
                System.out.println("인증 상태를 확인 중...");
                HttpRequest pollRequest = HttpRequest.newBuilder()
                        .uri(URI.create(AUTH_SERVER_URL + "/api/devices/poll/" + deviceCode))
                        .GET()
                        .build();
                HttpResponse<String> pollResponse = client.send(pollRequest, HttpResponse.BodyHandlers.ofString());

                if (pollResponse.statusCode() == 200 && pollResponse.body() != null && !pollResponse.body().isEmpty()) {
                    jwtToken = new JSONObject(pollResponse.body()).getString("token");
                    System.out.println("인증 성공! JWT 토큰을 발급받았습니다.");
                }
            }

            // 3. 인증 성공 후, 스트리밍 서버 시작 (기존 로직)
            // TODO: 발급받은 jwtToken을 AuthManager가 사용하도록 전달해야 함
            startStreamingServer(jwtToken);

        } catch (Exception e) {
            System.err.println("인증 과정 또는 서버 시작 중 오류 발생");
            e.printStackTrace();
        }
    }
    
    /**
     * EchoLink 스트리밍 시작
     * @param jwtToken
     * @throws IOException
     */
    private static void startStreamingServer(String jwtToken) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(loginPort)) {
            System.out.println("EchoLink 스트리밍 서버가 시작되었습니다...");
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("클라이언트 연결됨: " + clientSocket.getInetAddress().getHostAddress());
                // TODO: ClientHandler가 jwtToken을 사용하도록 생성자 등 수정 필요
                new Thread(new ClientHandler(clientSocket)).start();
            }
        }
    }
}