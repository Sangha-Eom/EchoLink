package Server;

import java.awt.AWTException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import org.json.JSONObject;

/**
 * 클라이언트로 받은 설정값 적용 및 스트리밍 시작
 * 각 기능 모듈들 관리
 * @author ESH
 */
public class ClientHandler implements Runnable {

	private final Socket clientSocket;
	private final AuthManager authManager;				// AuthManager 인스턴스

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
			String loginData = reader.readLine(); 
			if (loginData == null)	// 클라이언트 비정상 종료
				return;

			JSONObject loginJson = new JSONObject(loginData);
			String id = loginJson.getString("id");
			String password = loginJson.getString("password");

			if (!authManager.authenticate(id, password)) {	// 인증 실패
				writer.write("FAIL\n");
				writer.flush();
				System.out.println("로그인 실패: " + id);
				return;
			}

			writer.write("OK\n");
			writer.flush();
			System.out.println("로그인 성공: " + id);


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
			if (configData == null)
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
			StreamSessionManager streamManager = new StreamSessionManager(
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

		} catch (Exception e) {
			System.err.println("클라이언트 처리 중 오류 발생 (" 
					+ clientSocket.getInetAddress().getHostAddress() + "): " 
					+ e.getMessage());
			e.printStackTrace();	// 상세 오류 정보
		} finally {
			try {
				if (clientSocket != null && !clientSocket.isClosed()) {
					clientSocket.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
