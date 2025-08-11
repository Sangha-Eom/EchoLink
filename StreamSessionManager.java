package Server;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 스트리밍 세션을 관리하는 매니저 클래스
 * @author ESH
 */
public class StreamSessionManager {
	
	/*
	 *  추후 클라이언트로 부터
	 *  프레임, 비트레이트, 픽셀 포맷 지정 가능하도록 수정 
	 */
	public static int frameRate = 30;	// 기본 프레임
	public static int bitrate = 2000 * 1000;	// 기본 비트레이트 (2Mbps)
	public static int pixelFormat = 0;	// 0: 기본, 필요 시 avutil.AV_PIX_FMT_YUV420P 등 지정
	
	// 화면 크기
	public static Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	

    /**
     * 클라이언트 IP와 포트를 받아 스트리밍 세션을 시작
     * @param clientIp 클라이언트의 IP 주소
     * @param port 클라이언트 수신 포트 (UDP)
     */
    public static void startSession(String clientIp, int fps, int bitrate, int width, int height, int port) {
        System.out.println("스트리밍 세션 시작: " + clientIp + ":" + port);
        System.out.println("설정값: 해상도: " + width + "x" + height +
                           ", FPS: " + fps + ", 비트레이트: " + bitrate);

        // 캡처된 이미지 프레임을 담을 큐(임시:30개)
        BlockingQueue<BufferedImage> frameQueue = new LinkedBlockingQueue<>(30);

        // 화면 캡처 스레드 시작
        ScreenCapture capture = new ScreenCapture(frameQueue, frameRate);
        Thread captureThread = new Thread(capture);
        captureThread.start();

        // 인코더 및 전송 스레드 시작
        Encoder encoder = new Encoder(frameQueue, clientIp, port, 
        		screenSize.width, screenSize.height, frameRate, bitrate);
        
        
        // ✅ Android에서 연결 전에 변경 가능하도록 setter 사용
        encoder.setVideoBitrate(bitrate);
        encoder.setPixelFormat(pixelFormat);
        
        
        Thread encoderThread = new Thread(encoder);
        encoderThread.start();
    }
    
}
