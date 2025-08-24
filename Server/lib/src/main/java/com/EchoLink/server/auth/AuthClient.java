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
 * - 데스크탑 서버가 해당 이메일을 본인 계정과 매칭한 뒤 스트리밍 연결 허용
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
	 * 
	 * 
	 * @param sessionId
	 * @return
	 * @throws IOException
	 */
	public String validateSession(String sessionId) throws IOException  {
		
		String urlStr = authServerBaseUrl + "/api/sessions/" + sessionId;
		try {
			URL url = (new URI(urlStr)).toURL();
			HttpURLConnection connect = (HttpURLConnection) url.openConnection();
			connect.setRequestMethod("GET");
		}
		catch (URISyntaxException e) {
			e.printStackTrace();
		}

		int status = connect.getResponseCode();
		if (status == 200) {
			try (Scanner sc = new Scanner(connect.getInputStream())) {
				StringBuilder response = new StringBuilder();
				while (sc.hasNext()) response.append(sc.nextLine());
				JSONObject json = new JSONObject(response.toString());
				return json.getString("email");  // 세션에서 이메일 추출
			}
		} 
		else if (status == 404) {
			return null; // 세션 없음
		} 
		else {
			throw new IOException("Auth server error: " + status);
		}
		
	}
	
}
