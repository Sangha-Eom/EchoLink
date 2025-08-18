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

/**
 * 클라이언트로 받은 설정값 적용 및 스트리밍 시작
 * 각 기능 모듈들 관리
 * @author ESH
 */
public class ClientHandler implements Runnable {

	private final Socket clientSocket;
	private final AuthManager authManager;	// AuthManager 인스턴스
	private StreamSessionManager streamManager;	// 세션 매니저

	public ClientHandler(Socket socket) {
		this.clientSocket = socket;
		this.authManager = new AuthManager();	// Handler마다 독립적인 인스턴스 생성
	}

	@Override
	public void run() {
		try (
				BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))
				) {
			/*
			 *  1. 로그인 정보 수신 및 인증
			 */
			String tokenData = reader.readLine(); 
			if (tokenData == null)	// 클라이언트 비정상 종료
				return;

			JSONObject tokenJson = new JSONObject(tokenData);
			String jwt = tokenJson.getString("jwt"); // 클라이언트가 보낸 JWT 추출

			// authManage의 validateToken 메소드로 검증
			if (!authManager.validateToken(jwt)) {	// 인증 실패
				writer.write("FAIL\n");
				writer.flush();
				System.out.println("로그인(JWT 인증) 실패: " + authManager.getUsernameFromToken(jwt));
				return;
			}

			String username = authManager.getUsernameFromToken(jwt);
			writer.write("OK\n");
			writer.flush();
			System.out.println("로그인(JWT 인증) 성공: " + username);


			/*
			 *  2. 세션 설정값 수신
			 *  StreamSessionManager 인스턴스
			 *  클라이언트로부터 받은 설정값 수신
			 *  a. fps: 프레임
			 *  b. bitrate: 비트레이트
			 *  c. width: 가로
			 *  d. height: 세로
			 *  e. port: 클라이언트 UDP 포트 번호
			 */
			String configData = reader.readLine();
			if (configData == null)		// 설정값 받아오기 실패
				return;

			JSONObject configJson = new JSONObject(configData);
			int fps = configJson.getInt("fps");
			int bitrate = configJson.getInt("bitrate");
			int width = configJson.getInt("width");
			int height = configJson.getInt("height");
			int port = configJson.getInt("port");


			/*
			 *  3. StreamSessionManager를 통해 스트리밍 세션 시작
			 *  StreamSessionManager의 startSession이 스트림 스레드를 관리
			 */
			this.streamManager = new StreamSessionManager(
					clientSocket.getInetAddress().getHostAddress(),
					fps, bitrate, width, height, port
					);
			streamManager.startSession();


			/*
			 *  4. 입력 이벤트 처리 쓰레드
			 *  InputEventRecieiver를 통해 송/수신
			 */
			InputEventReceiver inputReceiver = new InputEventReceiver(clientSocket);
			new Thread(inputReceiver).start();

		} 
		catch (Exception e) {
			System.err.println("클라이언트 처리 중 오류 발생 (" 
					+ clientSocket.getInetAddress().getHostAddress() + "): " 
					+ e.getMessage());
			e.printStackTrace();	// 상세 오류 정보
		} 
		finally {
			try {
				if (clientSocket != null && !clientSocket.isClosed()) {
					clientSocket.close();
				}
				if (streamManager != null) {
					streamManager.stopSession(); // 모든 캡처/인코딩 스레드를 중지시키는 메소드
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		/* end */
	}
}
