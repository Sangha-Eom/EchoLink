package Server;

import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.Frame;
import org.bytedeco.ffmpeg.global.avutil;

import java.awt.image.BufferedImage;
import java.util.concurrent.BlockingQueue;

/**
 * 
 * 인코딩: BufferedImage -> H.264
 * 이후 UDP 포트로 전송
 * @author ESH
 */
public class Encoder implements Runnable {

    private final BlockingQueue<BufferedImage> frameQueue;	// 프레임 공유용 버퍼
    private final String clientIp;							// 클라이언트
    private final int port;									// UDP 포트
    private final int width;								// 화면 크기(가로)
    private final int height;								// " (세로)
    private final int frameRate;							// 프레임
    private int videoBitrate; 								// 비트레이트(영상 품질)
    private int pixelFormat; // 픽셀 포맷


    private volatile boolean running = true;
    
    /**
     * 생성자
     * @param frameQueue	프레임 공유용 버퍼
     * @param clientIp		클라이언트
     * @param port			UDP 전송 포트
     * @param width			가로
     * @param height		세로
     * @param fps		프레임
     * @param bitrate 비트레이트
     */
    public Encoder(BlockingQueue<BufferedImage> frameQueue,
                         String clientIp, int port,
                         int width, int height, int fps, int bitrate) {
    	
        this.frameQueue = frameQueue;
        this.clientIp = clientIp;
        this.port = port;
        this.width = width;
        this.height = height;
        this.frameRate = fps;
        this.videoBitrate = bitrate;
        
        this.pixelFormat = avutil.AV_PIX_FMT_YUV420P;   // 기본값
    }

    
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
            // 추후 안드로이드에서 설정 가능하도록 설정(비트레이트, 프레임)
            recorder.setFormat("flv"); // UDP에는 flv가 안정적
            recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);	// H.264 인코딩
            recorder.setFrameRate(frameRate);		// ★FPS 설정(현재:30fps)
            recorder.setVideoBitrate(videoBitrate); // ★비트레이트 설정
            recorder.setPixelFormat(pixelFormat); // H.264의 픽셀 포맷
            
            // ✅ Optional: 인코딩 지연 줄이기 위한 설정(딜레이 최소화)
            recorder.setVideoOption("tune", "zerolatency");
            recorder.setVideoOption("preset", "ultrafast");

            recorder.start();

            while (running) {
                BufferedImage image = frameQueue.take(); // 블로킹 방식
                Frame frame = converter.convert(image);
                recorder.record(frame);	// 인코딩 + 전송 담당
            }

            recorder.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
		
		/** 
		 * 비트레이트 변경 (bps 단위) 
		 * 재시작 후 반영
		 */
    public void setVideoBitrate(int videoBitrate) {
        this.videoBitrate = videoBitrate;
    }

    /** 
     * 픽셀 포맷 변경 (avutil.AV_PIX_FMT_XXX)
     * 재시작 후 반영 
     */
    public void setPixelFormat(int pixelFormat) {
        this.pixelFormat = pixelFormat;
    }
    
		/** 
     * 인코더 종료
     */
    public void stop() {
        running = false;
    }
}
