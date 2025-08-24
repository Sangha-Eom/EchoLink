package com.EchoLink.server.stream;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import java.awt.image.BufferedImage;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.bytedeco.ffmpeg.global.avutil;


/**
 * 영상, 음성 프레임을 각각의 큐에서 받아 인코딩
 * 영상: BufferedImage -> H.264
 * 음성: Frame -> AAC
 * 이후 하나의 스트림으로 합쳐(Muxing) 클라이언트에게 UDP 포트로 전송.
 * @author ESH
 */
public class Encoder implements Runnable {

	private final BlockingQueue<TimestampedFrame<BufferedImage>> videoFrameQueue;	// 영상 공유용 큐
	private final BlockingQueue<TimestampedFrame<Frame>> audioFrameQueue;			// 오디오 공유용 큐

	/*
	 * 영상용
	 */
	private final String clientIp;							// 클라이언트
	private final int port;									// UDP 포트
	private final int width;								// 화면 크기(가로)
	private final int height;								// " (세로)
	private final int frameRate;							// 프레임
	private int videoBitrate; 								// 비트레이트(영상 품질)

	private FFmpegFrameRecorder recorder;	// 영상/오디오 제공자
	private final ExecutorService executor = Executors.newFixedThreadPool(2); // 영상,음성 처리용 스레드 풀

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
	public Encoder(BlockingQueue<TimestampedFrame<BufferedImage>> videoQueue, BlockingQueue<TimestampedFrame<Frame>> audioQueue,
			String clientIp, int port, int width, int height, 
			int fps, int bitrate) {

		this.videoFrameQueue = videoQueue;
		this.audioFrameQueue = audioQueue;
		this.clientIp = clientIp;
		this.port = port;
		this.width = width;
		this.height = height;
		this.frameRate = fps;
		this.videoBitrate = bitrate;
	}


	@Override
	public void run() {
		String outputUrl = "udp://" + clientIp + ":" + port + "?pkt_size=1316&fifo_size=1000000";
		System.out.println("UDP 스트리밍(Encoder) 시작: " + outputUrl);

		// FFmpegFrameRecorder 설정
		// outputUrl에게 데이터 자동 전송
		// 오디오 채널(2=스테레오) 명시적 설정
		try {

			recorder = new FFmpegFrameRecorder(outputUrl, width, height, 2);

			/*
			 *  --- 비디오 설정 ---
			 */
			// H.264 코덱 설정
			// 추후 안드로이드에서 설정 가능하도록 설정(비트레이트, 프레임 등)
			recorder.setFormat("flv"); 				// UDP에는 flv가 안정적
			recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);		// H.264 표준 인코딩
			recorder.setFrameRate(frameRate);		// ★FPS 설정
			recorder.setVideoBitrate(videoBitrate); // ★비트레이트 설정
			recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P); 	// H.264 표준 픽셀 포맷

			// Optional: 인코딩 지연 줄이기 위한 설정(딜레이 최소화)
			recorder.setVideoOption("tune", "zerolatency");
			recorder.setVideoOption("preset", "ultrafast");


			/*
			 *  --- 오디오 설정 ---
			 */
			recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC); // AAC 코덱 사용
			recorder.setSampleRate(44100); 					// AudioCapture와 동일하게 설정
			recorder.setAudioBitrate(192000);				 // 192kbps


			// 인코더 실행
			System.out.println("인코더 시작 (영상+음성): " + outputUrl);
			recorder.start();


			/*
			 * 쓰레드 시작
			 */
			// 영상과 음성을 별도의 스레드에서 병렬로 처리하여 서로를 방해하지 않도록 함.
			Java2DFrameConverter converter = new Java2DFrameConverter();
			
			// 영상, 오디오 쓰레드
			executor.submit(() -> processVideoFrames(recorder, converter));	// 영상 쓰레드 시작
			executor.submit(() -> processAudioFrames(recorder));			// 오디오 쓰레드 시작


			// 메인 쓰레드
			// running 플래그가 false 될 때까지 대기
			while (running) {
				Thread.sleep(100);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			stop();
			System.out.println("Encoder 종료.");
		}

	}

	/**
	 * 영상 처리 쓰레드
	 * @param recorder
	 * @param converter
	 */
	private void processVideoFrames(FFmpegFrameRecorder recorder, Java2DFrameConverter converter) {
		while (running) {
			try {
				// '상자'를 꺼냄
				TimestampedFrame<BufferedImage> tsFrame = videoFrameQueue.take();
				Frame videoFrame = converter.convert(tsFrame.getFrame());

				// synchronized 블록으로 recorder 접근을 동기화(쓰레드 동시 접근 제한)
				synchronized (recorder) {
					// 타임스탬프 설정 (나노초 -> 마이크로초 변환)
					recorder.setTimestamp(tsFrame.getTimestamp() / 1000);
					recorder.record(videoFrame);
				}
			} catch (Exception e) {
				if (running) e.printStackTrace();
			}
		}
	}

	/**
	 * 오디오 처리 쓰레드
	 * @param recorder 
	 */
	private void processAudioFrames(FFmpegFrameRecorder recorder) {
		while (running) {
			try {
				// '상자'를 꺼냄
				TimestampedFrame<Frame> tsFrame = audioFrameQueue.take();

				// synchronized 블록으로 recorder 접근을 동기화(쓰레드 동시 접근 제한)
				synchronized (recorder) {
					// 타임스탬프 설정 (나노초 -> 마이크로초 변환)
					recorder.setTimestamp(tsFrame.getTimestamp() / 1000);
					recorder.record(tsFrame.getFrame());
				}
			} catch (Exception e) {
				if (running) e.printStackTrace();
			}
		}
	}

	/**
	 * 비트레이트를 동적으로 변경하는 메소드.(스트리밍 중)
	 * 
	 * FFmpegFrameRecorder는 스레드에 안전하지 않을 수 있으므로 동기화 처리.
	 * @param bitrate 새로운 비트레이트 값(bps 단위)
	 */
	public void setVideoBitrate(int bitrate) {
		try {
			synchronized (recorder) { 	// recorder 객체를 동기화하여 안전하게 접근
				this.videoBitrate = bitrate;
				recorder.setVideoBitrate(this.videoBitrate);
				System.out.println(">> 비트레이트가 " + bitrate + "bps로 변경되었습니다.");
			}
		} catch (Exception e) {
			System.err.println("비트레이트 변경 중 오류 발생: " + e.getMessage());
		}
	}



	/** 
	 * 인코더 종료
	 */
	public void stop() {
		running = false;
		executor.shutdownNow();
		try {
			if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
				System.err.println("인코더 스레드 풀이 정상적으로 종료되지 않았습니다.");
			}
		} catch (InterruptedException e) {
			executor.shutdownNow();
		}
	}
}

