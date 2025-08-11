package Server;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.BlockingQueue;

/**
 * 화면 캡처
 * 이후 인코더(EncoderWorker.java)에게 넘겨줌
 * @author ESH
 */
public class ScreenCapture implements Runnable {

    private final BlockingQueue<BufferedImage> frameQueue;	// 프레임 공유용 큐 + 쓰레드 블로킹 => Endcoder에게 넘김
    private final Rectangle captureArea;					// 캡처할 화면 영역
    private volatile boolean running = true;				// 쓰레드 실행 상태

    private final long frameIntervalMillis;					// 촬영 간격
    
    /**
     * 생성자
     * @param frameQueue 프레임 공유용 큐
     * @param frameRate 프레임
     */
    public ScreenCapture(BlockingQueue<BufferedImage> frameQueue, int fps, Dimension captureSize) {
        this.frameQueue = frameQueue;
        this.frameIntervalMillis = 1000/fps;

        this.captureArea = new Rectangle(captureSize);	// 모니터 사이즈(해상도)만큼 사진 캡처
    }

    @Override
    public void run() {
        try {
            Robot robot = new Robot();
            while (running) {
                long startTime = System.currentTimeMillis();

                BufferedImage screenshot = robot.createScreenCapture(captureArea);
                frameQueue.put(screenshot);  // 소비자에게 전달

                long elapsed = System.currentTimeMillis() - startTime;
                long sleepTime = frameIntervalMillis - elapsed;
                if (sleepTime > 0) {
                    Thread.sleep(sleepTime);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        running = false;
    }
}
