package Server;

import java.awt.AWTException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import org.json.JSONObject;

/**
 * 클라이언트로 받은 설정값 적용 및 스트리밍 시작
 * @author ESH
 */
public class ClientHandler implements Runnable {
    
    private final Socket clientSocket;
    private final StreamSessionManager streamManager;	// StreamSessionManager 인스턴스
    private final InputEventReceiver inputReceiver;		// InputEventReceiver 인스턴스
    private final AuthManager authManager;				// AuthManager 인스턴스
    
    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
		this.streamManager = new StreamSessionManager(null, 0, 0, 0, 0, 0);
		try {
			this.inputReceiver = new InputEventReceiver(socket);
		} catch (AWTException e) {
			e.printStackTrace();
		}
        this.authManager = new AuthManager();	// Handler마다 독립적인 인스턴스 생성
    }

    @Override
    public void run() {
        try (
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))
        ) {
            // 1. 로그인 정보 수신 및 인증
            String loginData = reader.readLine(); 
            JSONObject loginJson = new JSONObject(loginData);
            String id = loginJson.getString("id");
            String password = loginJson.getString("password");
            
            if (!AuthManager.authenticate(id, password)) {
                writer.write("FAIL\n");
                writer.flush();
                System.out.println("로그인 실패: " + id);
                return;
            }
            
            writer.write("OK\n");
            writer.flush();
            System.out.println("로그인 성공: " + id);
            
            
            // 2. 세션 설정값 수신
            String configData = reader.readLine();
            JSONObject configJson = new JSONObject(configData);
            
            int fps = configJson.getInt("fps");
            int bitrate = configJson.getInt("bitrate");
            int width = configJson.getInt("width");
            int height = configJson.getInt("height");
            int port = configJson.getInt("port");
            
            // 3. StreamSessionManager를 통해 스트리밍 세션 시작
            // 스트리밍 세션 시작 (StreamSessionManager의 startSession이 스트림 스레드를 관리)
            StreamSessionManager.startSession(
                clientSocket.getInetAddress().getHostAddress(),
                fps, bitrate, width, height, port
            );
            
            // 입력 이벤트를 처리할 스레드 시작
            // 클라이언트와 같은 소켓을 사용하거나, 별도의 소켓을 사용하도록 설계할 수 있습니다.
            // 여기서는 클라이언트와 스트림 설정값 수신에 사용한 소켓을 재사용합니다.
            inputReceiver = new InputEventReceiver(clientSocket);
            new Thread(inputReceiver).start();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (clientSocket != null) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
