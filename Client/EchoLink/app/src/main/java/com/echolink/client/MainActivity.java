package com.echolink.client;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;


/**
 * Main 코드
 *
 * 1. 메인화면의 "Google 계정으로 로그인" 버튼 클릭 시
 * LoginActivity.java 실행 (로그인 창)
 *
 * 2. JWT 토큰 저장 시
 * 토큰 확인 후 서버 접속 시작
 *
 * @author ESH
 */
public class MainActivity extends AppCompatActivity {

    // 🚨 중요: 이 주소는 실제 데스크톱 서버 PC의 IP 주소로 변경해야 합니다.
    private static final String AUTH_SERVER_URL = "http://192.168.0.1:8080";

    private Button loginButton;
    private String jwtToken;

    private Socket controlSocket;   // 제어용 소켓
    private BufferedWriter controlWriter;

    /**
     * 서버 토큰이 있을 시 바로 연결
     * 없을 시 로그인 창으로 이동하여 토큰 얻어오기.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loginButton = findViewById(R.id.login_button);
        loginButton.setOnClickListener(v -> {

            // SharedPreferences에서 이미 토큰이 있는지 확인
            SharedPreferences prefs = getSharedPreferences("EchoLinkPrefs", MODE_PRIVATE);
            jwtToken = prefs.getString("jwt_token", null);

            if (jwtToken != null) {
                // 이미 토큰이 있으면 바로 연결 시도
                Toast.makeText(this, "이미 로그인되어 있습니다.", Toast.LENGTH_SHORT).show();
                // connectToServer(token);
            } else {
                // 토큰이 없으면 로그인 액티비티 시작
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.putExtra("url", AUTH_SERVER_URL);
                startActivity(intent);
            }
        });
    }

    /**
     * 화면이 다시 보일 시 토큰 확인.
     */
    @Override
    protected void onResume() {
        super.onResume();
        // 화면이 다시 보일 때마다 토큰 확인
        checkTokenAndConnect();
    }

    /**
     *  토큰 확인 및 스트리밍 서버 연결
     */
    private void checkTokenAndConnect() {
        SharedPreferences prefs = getSharedPreferences("EchoLinkPrefs", MODE_PRIVATE);
        this.jwtToken = prefs.getString("jwt_token", null);

        if (this.jwtToken != null) {
            loginButton.setVisibility(View.GONE); // 토큰이 있으면 로그인 버튼 숨기기
            Toast.makeText(this, "인증 성공! 서버에 연결합니다.", Toast.LENGTH_SHORT).show();
            connectToStreamingServer(); // 스트리밍 서버 연결 시작
        } else {
            loginButton.setVisibility(View.VISIBLE); // 토큰 없으면 로그인 버튼 보이기
        }
    }

    /**
     *  스트리밍 서버 연결
     */
    private void connectToStreamingServer() {
        new Thread(() -> {
            try {
                // 1. 스트리밍 서버의 20805 포트로 TCP 제어 소켓 연결
                controlSocket = new Socket("192.168.0.1", 20805); // 🚨 실제 PC IP로 변경
                controlWriter = new BufferedWriter(new OutputStreamWriter(controlSocket.getOutputStream()));
                BufferedReader controlReader = new BufferedReader(new InputStreamReader(controlSocket.getInputStream()));

                // 2. JWT 전송
                JSONObject tokenJson = new JSONObject();
                tokenJson.put("jwt", this.jwtToken);
                controlWriter.write(tokenJson.toString() + "\n");
                controlWriter.flush();

                // 3. 서버 응답 확인
                String response = controlReader.readLine();
                if (response == null || !response.equals("OK")) {
                    Log.e("Connection", "JWT 인증 실패");
                    controlSocket.close();
                    return;
                }
                Log.d("Connection", "JWT 인증 성공");

                // 4. 스트리밍 설정값 전송
                int clientUdpPort = 5555; // 클라이언트가 스트림을 받을 UDP 포트
                JSONObject configJson = new JSONObject();
                configJson.put("width", 1280);
                configJson.put("height", 720);
                configJson.put("fps", 60);
                configJson.put("bitrate", 4000000); // 4Mbps
                configJson.put("port", clientUdpPort);
                controlWriter.write(configJson.toString() + "\n");
                controlWriter.flush();

                // 5. 스트림 재생 시작 (다음 단계에서 구현)
                runOnUiThread(() -> startStreamPlayback(clientUdpPort));

                // 6. 원격 제어 이벤트 리스너 설정 (다음 단계에서 구현)
                runOnUiThread(this::setupTouchListener);

            } catch (Exception e) {
                e.printStackTrace();
                Log.e("Connection", "서버 연결 오류: " + e.getMessage());
            }
        }).start();
    }




}