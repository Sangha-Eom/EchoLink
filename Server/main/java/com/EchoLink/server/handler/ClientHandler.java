package com.EchoLink.server.handler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import org.json.JSONObject;

import com.EchoLink.server.auth.AuthManager;
import com.EchoLink.server.remote.InputEventReceiver;
import com.EchoLink.server.stream.Encoder;

/**
 * 클라이언트별 연결 관리, 인증, 설정 수신, 세션 시작 및 종료 총괄 클래스.
 * 
 * 각 기능 모듈들 관리
 * 1. JWT 수신 및 인증
 * 2. 스트리밍 세션 설정값 수신 및 시작
 * 3. 입력 이벤트 시작
 * @author ESH
 */
public class ClientHandler implements Runnable {

	private final Socket clientSocket;
	private final String serverJwt;			// 서버로부터 발급받은 원본 JWT
	
	private StreamSessionManager streamManager;	// 세션 매니저
	private final AuthManager authManager;		// 인증 관리
	private final Encoder activateEncoder;		// 제어 관리 명령
	
	/**
	 * 생성자
	 * @param socket 연결
	 * @param serverJWT 서버로부터 발급받은 원본 JWT
	 */
	public ClientHandler(Socket socket, String serverJWT) {
		this.clientSocket = socket;
		this.serverJwt = serverJWT;
		
		this.authManager = new AuthManager();	// Handler마다 독립적인 인스턴스 생성
		this.activateEncoder = streamManager.getEncoder();
	}

	@Override
	public void run() {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))
			) {
			
			// 1. 클라이언트 로그인 인증 처리
			if (!handleAuthentication(reader, writer))
				return;	// 인증 실패 시 즉시 종료.
			

			// 2. 스트리밍 세션 설정 및 시작
			if (!setupAndStartStreamingSession(reader))
				return; // 세션 설정 실패 시 종료

			
			// 3. 원격 입력 처리 시작
			startInputReceiver();

		} 
		catch (Exception e) {
			System.err.println("클라이언트 처리 중 오류 발생 (" 
					+ clientSocket.getInetAddress().getHostAddress() + "): " 
					+ e.getMessage());
			e.printStackTrace();	// 상세 오류 정보
		} 
		finally {
			cleanupSession();
		}
		
	}


	/** 
	 * 1. 클라이언트의 로그인 정보(JWT) 수신 및 인증
	 * @return 인증 성공 시 true, 실패 시 false
	 */
	private boolean handleAuthentication(BufferedReader reader, BufferedWriter writer) throws IOException {
		String tokenData = reader.readLine();
		if (tokenData == null) 
			return false;

		JSONObject tokenJson = new JSONObject(tokenData);
		String clientJwt = tokenJson.getString("jwt");	// 클라이언트가 보낸 JWT

		if (!authManager.validateToken(this.serverJwt, clientJwt)) {	// 인증 실패
			writer.write("FAIL\n");
			writer.flush();
			System.out.println("로그인(JWT 인증) 실패: " + authManager.getUsernameFromToken(clientJwt));
			return false;
		}

		String username = authManager.getUsernameFromToken(clientJwt);
		writer.write("OK\n");
		writer.flush();
		System.out.println("로그인(JWT 인증) 성공: " + username);
		return true;
		
	}
	
	/**
	 * 2. 클라이언트로부터 스트리밍 설정값을 받아 세션 시작
	 * @return 세션 시작 성공 시 true, 실패 시 false
	 */
	private boolean setupAndStartStreamingSession(BufferedReader reader) throws IOException {
		String configData = reader.readLine();
		if (configData == null) return false;

		JSONObject configJson = new JSONObject(configData);
		int fps = configJson.getInt("fps");
		int bitrate = configJson.getInt("bitrate");
		int width = configJson.getInt("width");
		int height = configJson.getInt("height");
		int port = configJson.getInt("port");
		
		// 세션 시작
		this.streamManager = new StreamSessionManager(
				clientSocket.getInetAddress().getHostAddress(),
				fps, bitrate, width, height, port
		);
		streamManager.startSession();
		return true;
	}
	
	
	/**
	 * 3. 원격 입력 처리 쓰레드 시작
	 */
	private void startInputReceiver() throws Exception {
		InputEventReceiver inputReceiver = new InputEventReceiver(clientSocket, activateEncoder);
		new Thread(inputReceiver).start();
	}
	
	
	/**
	 * 4. 모든 세션 리소스를 정리 및 소켓 닫기
	 */
	private void cleanupSession() {
		if (streamManager != null) {
			streamManager.stopSession();
			System.out.println("스트리밍 세션이 종료되었습니다.");
		}
		try {
			if (clientSocket != null && !clientSocket.isClosed()) {
				clientSocket.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
