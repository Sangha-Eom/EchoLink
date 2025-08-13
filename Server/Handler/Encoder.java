package Server;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import java.awt.image.BufferedImage;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


/**
 * 영상, 음성 프레임을 각각의 큐에서 받아 인코딩
 * 영상: BufferedImage -> H.264
 * 음성: Frame -> AAC
 * 이후 하나의 스트림으로 합쳐(Muxing) 클라이언트에게 UDP 포트로 전송.
 * @author ESH
 */
public class Encoder implements Runnable {

	private final BlockingQueue<BufferedImage> videoFrameQueue;	// 영상 공유용 큐
	private final BlockingQueue<Frame> audioFrameQueue;			// 오디오 공유용 큐
	
	/*
	 * 영상용
	 */
	private final String clientIp;							// 클라이언트
	private final int port;									// UDP 포트
	private final int width;								// 화면 크기(가로)
	private final int height;								// " (세로)
	private final int frameRate;							// 프레임
	private int videoBitrate; 								// 비트레이트(영상 품질)
	private int pixelFormat;								// 픽셀 포맷
	
	
	private final ExecutorService executor = Executors.newFixedThreadPool(2); // 영상/음성 처리용 스레드 풀
	
	private volatile boolean running = true;	// 종료

	/**
	 * 생성자
	 * @param frameQueue	프레임 공유용 버퍼
	 * @param clientIp		클라이언트
	 * @param port			UDP 전송 포트
	 * @param width			가로
	 * @param height		세로
	 * @param fps			프레임
	 * @param bitrate		비트레이트
	 */
	public Encoder(BlockingQueue<BufferedImage> videoQueue, BlockingQueue<BufferedImage> frameQueue,
			String clientIp, int port, int width, int height, 
			int fps, int bitrate, int pixelFormat) {
		
		this.videoFrameQueue = videoQueue;
		this.videoFrameQueue = frameQueue;
		this.clientIp = clientIp;
		this.port = port;
		this.width = width;
		this.height = height;
		this.frameRate = fps;
		this.videoBitrate = bitrate;

		this.pixelFormat = pixelFormat;   // 기본값: avutil.AV_PIX_FMT_YUV420P
	}

	// 여기부터 Gemini 보고 수정 시작
	@Override
	public void run() {
		String outputUrl = "udp://" + clientIp + ":" + port;
		System.out.println("UDP 스트리밍(Encoder) 시작: " + outputUrl);

		Java2DFrameConverter converter = new Java2DFrameConverter();

		// ✅ FFmpegFrameRecorder 설정
		// outputUrl에게 데이터 자동 전송
		try (FFmpegFrameRecorder recorder = 
				new FFmpegFrameRecorder(outputUrl, width, height)) {

			// ✅ H.264 코덱 설정
			// 추후 안드로이드에서 설정 가능하도록 설정(비트레이트, 프레임 등)
			recorder.setFormat("flv"); 				// UDP에는 flv가 안정적
			recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);	// H.264 인코딩
			recorder.setFrameRate(frameRate);		// ★FPS 설정(현재:30fps)
			recorder.setVideoBitrate(videoBitrate); // ★비트레이트 설정
			recorder.setPixelFormat(pixelFormat); // H.264의 픽셀 포맷

			// ✅ Optional: 인코딩 지연 줄이기 위한 설정(딜레이 최소화)
			recorder.setVideoOption("tune", "zerolatency");
			recorder.setVideoOption("preset", "ultrafast");

			recorder.start();

			while (running) {
				BufferedImage image = videoFrameQueue.take(); // 블로킹 방식
				Frame frame = converter.convert(image);
				recorder.record(frame);	// 인코딩 + 전송 담당
			}

			recorder.stop();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}


	/** 
	 * 인코더 종료
	 */
	public void stop() {
		running = false;
	}
}
