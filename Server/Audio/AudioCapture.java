package Server;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import java.util.concurrent.BlockingQueue;

/**
 * 시스템에서 재생되는 오디오를 캡처하여 큐에 추가합니다.
 * FFmpeg를 사용하여 OS별 오디오 장치로부터 소리를 가져옵니다.
 * 
 * ✅ 중요: 위 코드의 audioDeviceName은 시스템마다 다릅니다. 
 * Windows의 경우, 'ffmpeg -list_devices true -f dshow -i dummy' 명령어를 
 * cmd에서 실행하여 사용 가능한 오디오 장치 목록을 확인하고 정확한 이름을 넣어주어야 합니다. 
 * (예: "마이크(Realtek High Definition Audio)", "스테레오 믹스(Realtek High Definition Audio)")
 * @author ESH
 */
public class AudioCapture implements Runnable {

    private final BlockingQueue<TimestampedFrame<Frame>> audioFrameQueue;
    private volatile boolean running = true;

    // 캡처할 오디오 장치 이름. OS에 따라 변경(추후 추가=>현재는 Windows만 지원)
    // Windows: "Stereo Mix" 장치가 활성화되어야 함("virtual-audio-capturer" 등)
    private final String audioDeviceName;	// OS별 오디오 장치 이름(Windows:"Stereo Mix")
    
    /**
     * 생성자
     * OS의 장치 이름을 받아 오디오 활성화
     * @param audioFrameQueue 오디오 프레임 큐
     * @param audioDevice OS별 오디오 장치 이름
     */
    public AudioCapture(BlockingQueue<TimestampedFrame<Frame>> audioFrameQueue, String audioDevice) {
        this.audioFrameQueue = audioFrameQueue;
        this.audioDeviceName = audioDevice;
    }

    @Override
    public void run() {
        /*
         *  OS에 맞는 오디오 입력 포맷을 지정
         *  Windows = "dshow"(DirectShow)
         */
        String inputFormat = System.getProperty("os.name").toLowerCase();
        if (inputFormat.contains("win"))
        	inputFormat = "dshow";
//        else if (inputFormat.contains("linux"))
//        	inputFormat = "pulse";
//        else if (inputFormat.contains("mac"))
//        	inputFormat = "pulse";
        else
        	inputFormat = "pulse";	// 다른 운영 체제 시 기본값:pulse

        try {
            /*
             *  FFmpegFrameGrabber를 사용하여 오디오 장치를 열기.
             */
            FFmpegFrameGrabber grabber = new FFmpegFrameGrabber("audio=" + audioDeviceName);
            grabber.setFormat(inputFormat);
            grabber.setSampleRate(44100); // 표준 샘플링 속도
            grabber.setAudioChannels(2);  // 스테레오
            grabber.start();

            System.out.println("오디오 캡처 시작: " + audioDeviceName);

            while (running) {
                Frame audioFrame = grabber.grab();	 // 오디오 프레임 캡처
                if (audioFrame != null) {
                	// 캡처한 오디오 프레임과 현재 시간을 TimestampedFrame으로 감싸서 큐에 추가
                    audioFrameQueue.put(new TimestampedFrame<>(audioFrame.clone(), System.nanoTime()));
                }
            }

            grabber.stop();
            grabber.release();
            System.out.println("오디오 캡처 종료.");

        } catch (Exception e) {
            System.err.println("오디오 캡처 중 심각한 오류 발생. 오디오 장치 이름(" + audioDeviceName + ")과 드라이버를 확인하세요.");
            e.printStackTrace();
        }
    }

    public void stop() {
        this.running = false;
    }
}
