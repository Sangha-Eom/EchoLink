package com.EchoLink.server.capture;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.EchoLink.server.stream.TimestampedFrame;

/**
 * 화면 캡처
 * 이후 인코더(EncoderWorker.java)에게 넘겨줌
 * @author ESH
 */
public class ScreenCapture implements Runnable {

	private final BlockingQueue<TimestampedFrame<BufferedImage>> frameQueue;	// 프레임 공유용 큐 + 쓰레드 블로킹 => Endcoder에게 넘김
	private final Rectangle captureArea;					// 캡처할 화면 영역
	private volatile boolean running = true;				// 쓰레드 실행 상태

	private final long frameIntervalMillis;					// 촬영 간격

	/**
	 * 생성자
	 * @param frameQueue 프레임 공유용 큐
	 * @param frameRate 프레임
	 */
	public ScreenCapture(BlockingQueue<TimestampedFrame<BufferedImage>> frameQueue, int fps, Dimension captureSize) {
		this.frameQueue = frameQueue;
		this.frameIntervalMillis = 1000/fps;

		this.captureArea = new Rectangle(captureSize);	// 모니터 사이즈(해상도)만큼 사진 캡처
	}

	@Override
	public void run() {
		Robot robot;
		try {
			robot = new Robot();

		} 
		catch (AWTException e) {
			System.err.println("Robot 객체 생성에 실패했습니다. GUI 환경이 아닐 수 있습니다.");
			e.printStackTrace();
			return; // run 메소드 종료
		}

		/*
		 * 화면 캡처
		 */
		long nextFrameTime = System.nanoTime();	// FPS 제어 변수

		while (running) {
			try {
				BufferedImage screenshot = robot.createScreenCapture(captureArea);
				// 캡처한 이미지와 현재 시간을 TimestampedFrame으로 감싸서 큐에 추가
				frameQueue.put(new TimestampedFrame<>(screenshot, System.nanoTime()));

				// 다음 프레임 시간까지 정확히 대기
				nextFrameTime += frameIntervalMillis * 1_000_000; // ms를 ns로 변환하여 더함
				long sleepTimeNs = nextFrameTime - System.nanoTime();

				if (sleepTimeNs > 0) {
					TimeUnit.NANOSECONDS.sleep(sleepTimeNs);
				}

			} 
			catch (InterruptedException e) {
				running = false;
				Thread.currentThread().interrupt(); 	// 스레드 중단 상태를 다시 설정
				System.out.println("화면 캡처 스레드가 중단되었습니다.");
				break;
			} 
			catch (Exception e) {
				System.err.println("화면 캡처 중 예상치 못한 오류 발생: " + e.getMessage());
			}
		}

	}


	/**
	 * 외부에서 캡처 쓰레드 중지
	 */
	public void stop() {
		running = false;
	}
}
