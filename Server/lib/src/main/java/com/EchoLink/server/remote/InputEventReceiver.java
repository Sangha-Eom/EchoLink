package com.EchoLink.server.remote;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.io.DataInputStream;
import java.io.InputStream;
import java.net.Socket;

import com.EchoLink.server.stream.Encoder;


/**
 * 클라이언트로부터 여러 이벤트를 받아 처리하는 클래스
 * 
 * 클라이언트: binary 값 수신
 * 서버: binary 값에 맞는 이벤트 처리
 * @author ESH
 */
public class InputEventReceiver implements Runnable {

    private Socket clientSocket;
    private Encoder encoder;
    private Robot robot;
    private volatile boolean running = true;

    /**
     * 생성자
     * @param clientSocket 클라이언트와 연결된 TCP 소켓
     */
    public InputEventReceiver(Socket clientSocket, Encoder encoder) throws AWTException {
        this.clientSocket = clientSocket;
        this.encoder = encoder;
        this.robot = new Robot();
    }

    @Override
    public void run() {
        try (InputStream is = clientSocket.getInputStream();
                DataInputStream dis = new DataInputStream(is)) {
               
               while (running) {
                   // 1. 이벤트 타입을 1바이트 읽는다.
                   byte eventType = dis.readByte();
                   
                   // 2. 타입에 따라 정해진 데이터를 읽어 처리한다.
                   switch (eventType) {
                       case 1: // MOUSE_MOVE
                           handleMouseMove(dis.readInt(), dis.readInt());
                           break;
                       case 2: // MOUSE_CLICK
                           handleMouseClick(dis.readInt());
                           break;
                       case 3: // KEY_PRESS
                           handleKeyPress(dis.readInt());
                           break;
                       case 4: // KEY_RELEASE
                           handleKeyRelease(dis.readInt());
                           break;
                       case 5: // CHANGE_BITRATE
                           handleBitrateChange(dis.readInt());
                           break;
                       case 6: // CHANGE_RESOLUTION
                           handleResolutionChange(dis.readInt(), dis.readInt());
                           break;
                       default:
                           System.out.println("알 수 없는 입력 타입: " + eventType);
                           break;
                   }
               }
           } catch (Exception e) {
               // 클라이언트 연결이 끊어지면 IOException이 발생하며 루프가 정상 종료됩니다.
               System.out.println("입력 스트림 종료: " + e.getMessage());
           } finally {
               stop();
           }
    }

	/**
	 * 마우스 이동 처리
	 * @param eventJson
	 */
    private void handleMouseMove(int x, int y) {
        robot.mouseMove(x, y);
    }

    /**
     * 마우스 클릭 처리
     * TODO:클라이언트: int 값 그대로 송신
     * @param eventJson
     */
    private void handleMouseClick(int buttonMask) {
        if (buttonMask != 0) {
            robot.mousePress(buttonMask);
            robot.mouseRelease(buttonMask);
        }
    }

    /**
     * 키 누름 처리
     * @param eventJson
     */
    private void handleKeyPress(int keyCode) {
        robot.keyPress(keyCode);
    }

    /**
     * 키 떼기 처리
     * @param eventJson
     */
    private void handleKeyRelease(int keyCode) {
        robot.keyRelease(keyCode);
    }
    
    /**
     * 해상도 변경 요청 처리
     * @param eventJson
     */
    private void handleResolutionChange(int width, int height) {
        if (encoder != null) {
            encoder.changeResolution(width, height);
        }
    }
    
    /**
     * 비트레이트 변경
     * @param eventJson
     */
    private void handleBitrateChange(int newBitrate) {
		if (encoder != null) {
			encoder.setVideoBitrate(newBitrate);
		}
	}
    
    /**
     *  스레드 종료
     */
    public void stop() {
        running = false;
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
