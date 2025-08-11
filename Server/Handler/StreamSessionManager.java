package Server;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 스트리밍 세션을 관리하는 매니저
 * @author ESH
 */
public class StreamSessionManager {
	
	/*
	 *  추후 클라이언트로 부터
	 *  프레임, 비트레이트, 픽셀 포맷 지정 가능하도록 수정 
	 */
    private final String clientIp;
    private final int port;
    private final int fps;
    private final int bitrate;
    private final int width;	// 스트리밍 요청 가로 길이
    private final int height;	// 스트리밍 요청 세로 길이
	private final Dimension serverScreenSize;	// 서버의 실제 화면 크기
	private final int pixelFormat;	// 픽셀 포맷(향후 확장용)
	
	/**
	 * 생성자
	 * 스트리밍에 필요한 설정값을 외부(ClientHandler)로 부터 받아 적용
	 * @param clientIp 	클라이언트 IP 주소
	 * @param fps		프레임
	 * @param bitrate	비트레이트
	 * @param width		요청 해상도 가로
	 * @param height	요청 해상도 세로
	 * @param port		클라이언트 수신 포트(UDP)
	 */
	public StreamSessionManager(String clientIp, int fps, int bitrate, 
			int width, int height, int port) {
		
		this.clientIp = clientIp;
		this.fps = fps;
        this.bitrate = bitrate;
        this.width = width;
        this.height = height;
        this.port = port;
        
        // 서버 화면 크기 내부적 초기화
        serverScreenSize = Toolkit.getDefaultToolkit().getScreenSize();
        pixelFormat = 0;	// 0: 기본, 필요 시 avutil.AV_PIX_FMT_YUV420P 등 지정
	}

    /**
     * 클라이언트 IP와 포트를 받아 스트리밍 세션을 시작
     * @param clientIp 클라이언트의 IP 주소
     * @param port 클라이언트 수신 포트 (UDP)
     */
    public void startSession() {
        try {
			System.out.println("스트리밍 세션 시작: " + clientIp + ":" + port);
	        System.out.println("설정값: 해상도: " + width + "x" + height +
                    ", FPS: " + fps + ", 비트레이트: " + bitrate);
		} catch (Exception e) {
			e.printStackTrace();
		}


        // 캡처된 이미지 프레임을 담을 큐 (임시: 30)
        BlockingQueue<BufferedImage> frameQueue = new LinkedBlockingQueue<>(30);

        // 화면 캡처 스레드 시작
        ScreenCapture capture = new ScreenCapture(frameQueue, fps, serverScreenSize);
        Thread captureThread = new Thread(capture);
        captureThread.start();

        // 인코더 및 전송 스레드 시작
        Encoder encoder = new Encoder(
        		frameQueue, 
        		clientIp, 
        		port, 
        		width, 	// 인코딩 목표 너비
        		height, // 인코딩 목표 높이
        		fps, 
        		bitrate);
        
        
        // ✅ Android에서 연결 전에 변경 가능하도록 setter 사용
        encoder.setVideoBitrate(bitrate);
        encoder.setPixelFormat(pixelFormat);
        
        
        Thread encoderThread = new Thread(encoder);
        encoderThread.start();
    }
    
}
