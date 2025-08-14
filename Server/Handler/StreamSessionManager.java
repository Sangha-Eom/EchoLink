package Server;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.bytedeco.javacv.Frame;

/**
 * 스트리밍 세션을 관리하는 매니저
 * 영상, 오디오
 * @author ESH
 */
public class StreamSessionManager {
	
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
        this.serverScreenSize = Toolkit.getDefaultToolkit().getScreenSize();
        this.pixelFormat = 0;	// 0: 기본, 필요 시 avutil.AV_PIX_FMT_YUV420P 등 지정
        
	}

	
    /**
     * 클라이언트 IP와 포트를 받아 스트리밍 세션을 시작
     * @param clientIp 클라이언트의 IP 주소
     * @param port 클라이언트 수신 포트 (UDP)
     */
    public void startSession() {
    	/*
    	 * 1. 설정값 적용 확인
    	 * 영상, 오디오 큐 생성
    	 * 이후 영상, 오디오 쓰레드 시작
    	 */
        try {
			System.out.println("스트리밍 세션 시작: " + clientIp + ":" + port);
	        System.out.println("설정값: 해상도: " + width + "x" + height +
                    ", FPS: " + fps + ", 비트레이트: " + bitrate);
		} catch (Exception e) {
			e.printStackTrace();
		}
        
        BlockingQueue<BufferedImage> frameQueue = new LinkedBlockingQueue<>(60); // 캡처된 이미지 프레임을 담을 큐 (임시: 30)
        BlockingQueue<Frame> audioQueue = new LinkedBlockingQueue<>(200);	// 오디오 큐
        
        // 영상 쓰레드
        ScreenCapture screenCapture = new ScreenCapture(frameQueue, fps, serverScreenSize);
        new Thread(screenCapture, "ScreenCapture-Thread").start();
        
        // 오디오 쓰레드
        String audioDevice = AudioDeviceManager.windowsfindOutputDeviceName();	// windows의 실제 장치 이름으로 전환
        if (audioDevice != null && !audioDevice.isEmpty()) {
            AudioCapture audioCapture = new AudioCapture(audioQueue, audioDevice);
            new Thread(audioCapture, "AudioCapture-Thread").start();
        }
        else {
            System.err.println("오디오 장치를 찾지 못해 오디오 캡처를 시작하지 않습니다.");
            // (선택) 오디오 큐에 빈 프레임을 주기적으로 넣어 인코더가 멈추지 않게 할 수도 있습니다.
            // 여기서는 오디오 없이 영상만 스트리밍되도록 합니다.
        }


        
        /*
         *  2. 인코더 쓰레드 시작
         *  영상, 오디오 큐 사용
         */
        Encoder encoder = new Encoder(
        		frameQueue, audioQueue,
        		clientIp, port, 
        		width, 	// 인코딩 목표 너비
        		height, // 인코딩 목표 높이
        		fps, bitrate);
        
        new Thread(encoder, "Encoder-Thread").start();
    }
    
}
