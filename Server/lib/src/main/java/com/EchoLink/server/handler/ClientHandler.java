package com.EchoLink.server.handler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

import org.json.JSONException;
import org.json.JSONObject;

import com.EchoLink.server.auth.AuthManager;
import com.EchoLink.server.config.ServerConfig;
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

	private final Socket clientSocket;		// 연결 Socket
	private final String serverJwt;			// 서버로부터 발급받은 원본 JWT
	private StreamSessionManager streamManager;	// 스트리밍 세션 매니저
	private final AuthManager authManager;		// 인증 관리

	// 연결 타임아웃 상수 (15초)
	private static final int SOCKET_TIMEOUT_MS = 15000;

    /**
     * 생성자
     * @param config StreamingServerService로부터 주입받은 ServerConfig 객체
     */
    public ClientHandler(Socket socket, String serverJWT, ServerConfig config) {
        this.clientSocket = socket;
        this.serverJwt = serverJWT;
        // 주입받은 config 객체로부터 jwtSecret 값을 가져와 AuthManager를 생성합니다.
        this.authManager = new AuthManager(config.getJwtSecret());
    }


	@Override
	public void run() {
		try {

			clientSocket.setSoTimeout(SOCKET_TIMEOUT_MS); // 읽기 타임아웃 설정

			// UTF-8 인코딩을 명시하여 문자 깨짐 방지
			BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8));


			// 1. 클라이언트 로그인 인증 처리 (handshake)
			if (!handleAuthentication(reader, writer)) {
				System.err.println("인증 실패: " + clientSocket.getInetAddress().getHostAddress());
				return;
			}

			// 2. 스트리밍 세션 설정 및 시작
			if (!setupAndStartStreamingSession(reader)) {
				System.err.println("스트리밍 세션 설정 실패: " + clientSocket.getInetAddress().getHostAddress());
				return;
			}

			// 3. 원격 입력 처리 시작 (이후 모든 통신은 InputEventReceiver가 담당)
			// 	+ ClientHandler는 더 이상 소켓을 직접 읽지 않으므로 타임아웃 해제.
			clientSocket.setSoTimeout(0);
			startInputReceiver();

		} 
		catch (SocketTimeoutException e) {
			// 타임아웃 예외 처리
			System.err.println("클라이언트 타임아웃 (" + SOCKET_TIMEOUT_MS + "ms): "
					+ clientSocket.getInetAddress().getHostAddress() + ". 연결을 종료합니다.");
		} 
		catch (IOException e) {
			// 네트워크 오류 또는 클라이언트의 갑작스러운 연결 종료 처리
			System.err.println("네트워크 오류 발생 ("
					+ clientSocket.getInetAddress().getHostAddress() + "): "
					+ e.getMessage());
		} 
		catch (Exception e) {
			// 그 외 예상치 못한 모든 예외 처리
			System.err.println("클라이언트 처리 중 예외 발생 ("
					+ clientSocket.getInetAddress().getHostAddress() + "): "
					+ e.getMessage());
			e.printStackTrace(); // 상세 오류 정보
		} 
		finally {
			// 어떤 상황에서든 cleanupSession이 호출되도록 보장
			cleanupSession();
		}
	}


	/** 
	 * 1. 클라이언트의 로그인 정보(JWT) 수신 및 인증
	 * 
	 * 1단계: JWT 서명 검증
	 * 2단계: 서버와 클라리언트의 사용자 계정 일치 확인
	 * @return 인증 성공 시 true, 실패 시 false
	 */
	private boolean handleAuthentication(BufferedReader reader, BufferedWriter writer) throws IOException {

		try {

			String handshakeData = reader.readLine();
			if (handshakeData == null) {
				System.err.println("핸드셰이크 데이터 수신 실패 (클라이언트가 즉시 연결 종료).");
				return false;
			}

			// 수신한 데이터가 유효한 JSON인지, 'jwt' 키를 포함하는지 명시적으로 검증
			JSONObject tokenJson = new JSONObject(handshakeData);
			if (!tokenJson.has("jwt")) {
				writer.write("FAIL: JWT token not found in handshake.\n");
				writer.flush();
				System.err.println("로그인 실패: 핸드셰이크에 JWT 토큰이 없습니다.");
				return false;
			}
			String clientJwt = tokenJson.getString("jwt");	// 클라이언트가 보낸 JWT

			// 1단계: JWT 서명 검증
			if (!authManager.validateToken(this.serverJwt, clientJwt)) {
				writer.write("FAIL\n");
				writer.flush();
				System.out.println("로그인 실패(토큰 무효): " + authManager.getUsernameFromToken(clientJwt));
				return false;
			}

			// 2단계: 서버와 클라이언트의 사용자 계정(이메일) 일치 확인
			String serverUserEmail = authManager.getUsernameFromToken(this.serverJwt);
			String clientUserEmail = authManager.getUsernameFromToken(clientJwt);

			if (!serverUserEmail.equals(clientUserEmail)) {
				writer.write("FAIL: User mismatch.\n");
				writer.flush();
				System.out.println("로그인 실패: 서버와 클라이언트의 사용자 계정이 일치하지 않습니다.");
				System.out.println("서버 계정: " + serverUserEmail + ", 클라이언트 계정: " + clientUserEmail);
				return false;
			}

			// 모든 인증 성공
			writer.write("OK\n");
			writer.flush();
			System.out.println("로그인 인증 성공: " + clientUserEmail);
			return true;
		}
		catch (JSONException e) {
			// 클라이언트가 비정상적인 데이터(JSON 형식이 아님)를 보냈을 경우
			writer.write("FAIL: Invalid handshake format.\n");
			writer.flush();
			System.err.println("로그인 실패: 클라이언트가 비정상적인 핸드셰이크 데이터를 전송했습니다. " + e.getMessage());
			return false;
		}
	}


	/**
	 * 2. 클라이언트로부터 스트리밍 설정값을 받아 세션 시작
	 * @return 세션 시작 성공 시 true, 실패 시 false
	 */
	private boolean setupAndStartStreamingSession(BufferedReader reader) throws IOException {
		
		String configData = reader.readLine();
		if (configData == null) 
			return false;

		try {
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
		} catch (JSONException e) {
			System.err.println("스트리밍 설정 실패: 클라이언트가 비정상적인 설정 데이터를 전송했습니다. " + e.getMessage());
			return false;
		}
	}


	/**
	 * 3. 원격 입력 처리 쓰레드 시작
	 */
	private void startInputReceiver() throws Exception {
		Encoder activeEncoder = streamManager.getEncoder();
		InputEventReceiver inputReceiver = new InputEventReceiver(clientSocket, activeEncoder);
		Thread inputThread = new Thread(inputReceiver, "InputReceiver-" + clientSocket.getInetAddress().getHostAddress());
		inputThread.start();
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
