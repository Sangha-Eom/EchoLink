package com.EchoLink.server.auth;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Scanner;

import org.json.JSONObject;

/**
 * 인증 된 계정에게 스트리밍 연결 허용 클래스
 * 
 * - 데스크탑 서버가 모바일로부터 받은 sessionID를 인증 서버에 검증 요청
 * - 인증 서버가 유효하면 사용자 이메일 반환
 * @author ESH
 */
public class AuthClient {

	private final String authServerBaseUrl;

	/**
	 * 생성자
	 * @param authServerBaseUrl
	 */
	public AuthClient(String authServerBaseUrl) {
		this.authServerBaseUrl = authServerBaseUrl;
	}

	/**
	 * 세션ID를 인증서버에 검증 메소드
	 * 
	 * @param sessionId 클라이언트가 보낸 세션 ID
	 * @return 세션 유효 시 이메일,
	 * 			없을 시 null
	 */
	public String validateSession(String sessionId) throws URISyntaxException, IOException {
		
		String urlStr = authServerBaseUrl + "/api/sessions/" + sessionId;
	    HttpURLConnection connect = null;

        try {
            URL url = (new URI(urlStr)).toURL();
            connect = (HttpURLConnection) url.openConnection();
            connect.setRequestMethod("GET");

            int status = connect.getResponseCode();
            if (status == 200) {
                try (Scanner sc = new Scanner(connect.getInputStream())) {
                    StringBuilder response = new StringBuilder();
                    while (sc.hasNext()) response.append(sc.nextLine());
                    JSONObject json = new JSONObject(response.toString());
                    return json.getString("email");
                }
            } else if (status == 404) {
                return null; // 세션 없음
            } else {
                throw new IOException("Auth server error: " + status);
            }

        } catch (Exception e) {
            throw new IOException("세션 검증 실패: " + e.getMessage(), e);
        } finally {
            if (connect != null) connect.disconnect();
        }
		
	}
	
}
