package Server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * 서버(데스크톱)의 시스템의 오디오 장치를 검색하고
 * 시스템 사운드 출력(스테레오 믹스 등)을 캡처하는 데 적합한 장치 이름을 찾는 유틸리티 클래스.
 * @author ESH
 */
public class AudioDeviceManager {

    /**
     * FFmpeg를 이용해 시스템의 오디오 장치 목록을 조회하고,
     * "Stereo Mix" 또는 "Loopback" 등 시스템 오디오 출력에 해당하는 장치 이름을 반환.
     * @return 찾은 오디오 장치 이름. 적합한 장치를 찾지 못하면 null을 반환합니다.
     */
    public static String findOutputDeviceName() {
        // Windows 운영체제에서만 이 로직을 실행
        String os = System.getProperty("os.name").toLowerCase();
        if (!os.contains("win")) {
            System.out.println("Windows가 아닌 환경에서는 오디오 장치 자동 검색을 지원하지 않습니다. 'pulse' 또는 'default'를 시도합니다.");
            // Linux 등 다른 OS를 위한 기본값 반환
            return "pulse";
        }

        try {
            // FFmpeg를 실행하여 dshow(DirectShow) 장치 목록을 가져오는 명령어
            ProcessBuilder processBuilder = new ProcessBuilder(
                "ffmpeg", "-list_devices", "true", "-f", "dshow", "-i", "dummy"
            );
            
            // 에러 스트림을 통해 장치 목록이 출력되므로, 리다이렉트 설정
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            List<String> deviceLines = new ArrayList<>();
            // 프로세스의 출력 결과를 라인별로 읽어 리스트에 저장
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[FFmpeg-devices] " + line); // 디버깅을 위해 전체 목록 출력
                    deviceLines.add(line);
                }
            }

            process.waitFor(); // 프로세스가 종료될 때까지 대기

            // 장치 목록에서 "Stereo Mix"를 포함하는 라인 찾기
            for (String line : deviceLines) {
                // FFmpeg 출력 형식: ... "Stereo Mix (Realtek High Definition Audio)" (audio)
                if (line.contains("(audio)") && line.toLowerCase().contains("stereo mix")) {
                    // 따옴표 사이의 장치 이름을 추출
                    int firstQuote = line.indexOf("\"");
                    int secondQuote = line.lastIndexOf("\"");
                    if (firstQuote != -1 && secondQuote > firstQuote) {
                        String deviceName = line.substring(firstQuote + 1, secondQuote);
                        System.out.println(">> 발견된 시스템 오디오 출력 장치: " + deviceName);
                        return deviceName;
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("FFmpeg를 통해 오디오 장치를 검색하는 중 오류 발생. FFmpeg가 시스템 경로(PATH)에 설치되어 있는지 확인하세요.");
            e.printStackTrace(); // 상세 오류 확인 필요 시
        }
        
        System.err.println(">> 경고: 시스템 오디오 출력 장치('Stereo Mix')를 자동으로 찾지 못했습니다. 오디오 캡처가 동작하지 않을 수 있습니다.");
        return null; // 적합한 장치를 찾지 못한 경우
    }
}
