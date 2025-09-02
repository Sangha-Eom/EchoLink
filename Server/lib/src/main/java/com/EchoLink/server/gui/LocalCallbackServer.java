package com.EchoLink.server.gui;

import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
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
            String query = exchange.getRequestURI().getQuery();
            if (query != null && query.contains("code=")) {
                authCode = query.substring(query.indexOf("code=") + 5).split("&")[0];
                String response = "<h1>인증 성공!</h1><p>앱으로 돌아가세요.</p>";
                exchange.sendResponseHeaders(200, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            } else {
                String error = "<h1>인증 실패</h1><p>오류가 발생했습니다. 다시 시도해주세요.</p>";
                exchange.sendResponseHeaders(400, error.length());
                 try (OutputStream os = exchange.getResponseBody()) {
                    os.write(error.getBytes());
                }
            }
            latch.countDown();
        });
        server.start();
        latch.await(); // 코드를 받을 때까지 대기
        server.stop(0);
        return authCode;
    }
}