package com.EchoLink.server.gui;

import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

/** 
 * Google OAuth2 콜백(인증 응답)을 받기 위한 로컬 HTTP 서버
 */
public class LocalCallbackServer {
	
    private final int port;
    private String authCode;
    private final CountDownLatch latch = new CountDownLatch(1);

    public LocalCallbackServer(int port) {
        this.port = port;
    }

    public String waitForCode() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", exchange -> {
            try {
                String query = exchange.getRequestURI().getQuery();
                if (query != null && query.contains("code=")) {
                    authCode = query.substring(query.indexOf("code=") + 5).split("&")[0];
                    String response = "<h1>인증 성공!</h1><p>앱으로 돌아가세요. 이 창을 닫아도 됩니다.</p>";
                    byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(200, responseBytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(responseBytes);
                    }
                } else {
                    String error = "<h1>인증 실패</h1><p>오류가 발생했습니다. 다시 시도해주세요.</p>";
                    byte[] errorBytes = error.getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(400, errorBytes.length);
                     try (OutputStream os = exchange.getResponseBody()) {
                        os.write(errorBytes);
                    }
                }
            } catch (Exception e) {
                // 오류가 발생하더라도 로그만 남기고 무시하여 앱이 멈추는 것을 방지
                e.printStackTrace();
            } finally {
                // ⭐중요: 성공하든, 실패하든, 오류가 발생하든 반드시 latch를 countDown하여
                // 대기 중인 메인 스레드를 깨움.
                latch.countDown();
            }
        });
        server.start();
        latch.await(); // 코드를 받거나, 오류가 발생하여 latch가 countDown 될 때까지 대기
        server.stop(0);
        return authCode;
    }
}
