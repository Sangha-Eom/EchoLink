package Server;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import org.json.JSONObject;

/**
 * 클라이언트로부터 마우스/키보드 입력을 받아 처리하는 클래스
 * @author ESH
 */
public class InputEventReceiver implements Runnable {

    private Socket clientSocket;
    private Robot robot;
    private volatile boolean running = true;

    /**
     * 생성자
     * @param clientSocket 클라이언트와 연결된 TCP 소켓
     */
    public InputEventReceiver(Socket clientSocket) throws AWTException {
        this.clientSocket = clientSocket;
        this.robot = new Robot();
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String line;
            while (running && (line = reader.readLine()) != null) {
                try {
                    JSONObject eventJson = new JSONObject(line);
                    String eventType = eventJson.getString("type");

                    switch (eventType) {
                        case "MOUSE_MOVE":
                            handleMouseMove(eventJson);
                            break;
                        case "MOUSE_CLICK":
                            handleMouseClick(eventJson);
                            break;
                        case "KEY_PRESS":
                            handleKeyPress(eventJson);
                            break;
                        case "KEY_RELEASE":
                            handleKeyRelease(eventJson);
                            break;
                        default:
                            System.out.println("알 수 없는 입력 타입: " + eventType);
                            break;
                    }
                } catch (Exception e) {
                    System.err.println("입력 이벤트 처리 중 오류 발생: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            stop();
        }
    }

    // 마우스 이동 처리
    private void handleMouseMove(JSONObject eventJson) {
        int x = eventJson.getInt("x");
        int y = eventJson.getInt("y");
        robot.mouseMove(x, y);
    }

    // 마우스 클릭 처리
    private void handleMouseClick(JSONObject eventJson) {
        String button = eventJson.getString("button");
        int mask = switch (button) {
            case "LEFT" -> InputEvent.BUTTON1_DOWN_MASK;
            case "RIGHT" -> InputEvent.BUTTON3_DOWN_MASK;
            default -> 0;
        };
        if (mask != 0) {
            robot.mousePress(mask);
            robot.mouseRelease(mask);
        }
    }

    // 키 누름 처리
    private void handleKeyPress(JSONObject eventJson) {
        int keyCode = eventJson.getInt("keyCode");
        robot.keyPress(keyCode);
    }

    // 키 떼기 처리
    private void handleKeyRelease(JSONObject eventJson) {
        int keyCode = eventJson.getInt("keyCode");
        robot.keyRelease(keyCode);
    }

    // 스레드 종료
    public void stop() {
        running = false;
        try {
            if (clientSocket != null) {
                clientSocket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
