package com.EchoLink.server.gui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;

import com.EchoLink.server.StreamingServerService;


/**
 * GUI 조작 클래스
 * 
 * FXML의 디자인, 동작 설정(버튼 클릭, 텍스트 변경 등)
 * Route: MainView.fxml
 */
@Component
public class MainController {

    // FXML에서 추가한 UI 요소들을 멤버 변수로 선언
    @FXML private ListView<String> deviceListView;
    @FXML private ComboBox<String> resolutionComboBox;
    @FXML private Slider bitrateSlider;
    @FXML private ComboBox<String> fpsComboBox;
    @FXML private Button startButton;
    @FXML private Button stopButton;
    @FXML private Label statusLabel;
    @FXML private Button logoutButton;
    
    @Autowired
    private StreamingServerService streamingServerService; // Spring의존성 주입(DI)
    
    private GuiService guiService;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private String jwtToken; // 로그인 시 GuiService로부터 받아야 함
    
    // GuiService로부터 인스턴스를 전달받기 위한 메소드
    public void setGuiService(GuiService guiService) {
        this.guiService = guiService;
    }
    
    /**
     * GuiService로부터 JWT 토큰을 전달받는 메소드 (GuiService에서 호출)
     * @param token
     */
    public void setJwtToken(String token) {
        this.jwtToken = token;
        fetchOnlineDevices(); // 토큰을 받은 후 기기 목록을 불러옵니다.
    }
    
    /**
     * 
     */
    @FXML
    public void initialize() {
        // 해상도 옵션 초기화
        resolutionComboBox.setItems(FXCollections.observableArrayList(
                "1920x1080", "1280x720", "854x480"
        ));
        resolutionComboBox.getSelectionModel().selectFirst();

        // FPS 옵션 초기화
        fpsComboBox.setItems(FXCollections.observableArrayList(
                "60", "30"
        ));
        fpsComboBox.getSelectionModel().selectFirst();

        statusLabel.setText("설정을 선택하고 스트리밍을 시작하세요.");
    }

    /**
     *  온라인 기기 목록을 서버에서 가져와 ListView에 표시
     */
    private void fetchOnlineDevices() {
        if (jwtToken == null) {
            Platform.runLater(() -> statusLabel.setText("오류: 인증 토큰이 없습니다."));
            return;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/devices"))
                .header("Authorization", "Bearer " + jwtToken)
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(this::updateDeviceList)
                .exceptionally(e -> {
                    e.printStackTrace();
                    Platform.runLater(() -> statusLabel.setText("기기 목록을 불러오는 데 실패했습니다."));
                    return null;
                });
    }
    
    /**
     *  API 응답을 파싱하여 UI 업데이트
     * @param responseBody
     */
    private void updateDeviceList(String responseBody) {
        Platform.runLater(() -> {
            deviceListView.getItems().clear();
            JSONArray devices = new JSONArray(responseBody);
            for (int i = 0; i < devices.length(); i++) {
                JSONObject device = devices.getJSONObject(i);
                String deviceName = device.getString("deviceName");
                String ipAddress = device.getString("ipAddress");
                deviceListView.getItems().add(deviceName + " (" + ipAddress + ")");
            }
        });
    }
    
    // 로그아웃 버튼 클릭 시 호출될 메소드
    @FXML
    private void handleLogout() {
        if (guiService != null) {
            guiService.handleLogout();
        } else {
            System.err.println("오류: GuiService가 MainController에 주입되지 않았습니다.");
        }
    }
    
    /**
     * 
     */
    @FXML
    private void handleStartStreaming() {
    	// ListView에서 선택된 기기의 IP 주소를 가져옵니다.
        String selectedDevice = deviceListView.getSelectionModel().getSelectedItem();
        if (selectedDevice == null) {
            statusLabel.setText("오류: 스트리밍을 시작할 기기를 선택하세요.");
            return;
        }
        // "DeviceName (192.168.0.1)" 형태에서 IP 주소만 추출
        String clientIp = selectedDevice.substring(selectedDevice.indexOf("(") + 1, selectedDevice.indexOf(")"));

        String selectedResolution = resolutionComboBox.getValue();
        int bitrate = (int) bitrateSlider.getValue();
        int fps = Integer.parseInt(fpsComboBox.getValue());

        // StreamingServerService의 세션 시작 메소드 호출
        streamingServerService.startStreamingSession(clientIp, selectedResolution, bitrate, fps);
        
        statusLabel.setText(String.format(
            "%s로 스트리밍 시작: %s, %d Mbps, %d FPS",
            clientIp, selectedResolution, bitrate, fps
        ));
        
        // UI 상태 변경
        startButton.setDisable(true);
        stopButton.setDisable(false);
        logoutButton.setDisable(true); // 스트리밍 중에는 로그아웃 비활성화
    }

    
    /**
     * 
     */
    @FXML
    private void handleStopStreaming() {
        // StreamingServerService의 세션 중지 메소드 호출
        streamingServerService.stopStreamingSession();

        statusLabel.setText("스트리밍이 중지되었습니다. 다시 시작할 수 있습니다.");
        
        // UI 상태 변경
        startButton.setDisable(false);
        stopButton.setDisable(true);
        logoutButton.setDisable(false); // 로그아웃 다시 활성화
    }
    
}
