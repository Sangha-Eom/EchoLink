package Server;

/**
 * 캡처된 데이터 프레임과 타임스탬프를 함께 저장하는 래퍼 클래스
 * 제네릭을 사용하여 BufferedImage와 Frame 모두 담을 수 있습니다.
 * 
 * 장시간 스트리밍 시 소리와 화면 싱크 어긋나는 현상 보완 클래스
 * @param <T> 프레임 데이터의 타입 (e.g., BufferedImage, Frame)
 */
public class TimestampedFrame<T> {
    private final T frame;
    private final long timestamp; // 캡처된 시점의 나노초 시간

    public TimestampedFrame(T frame, long timestamp) {
        this.frame = frame;
        this.timestamp = timestamp;
    }

    public T getFrame() {
        return frame;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
