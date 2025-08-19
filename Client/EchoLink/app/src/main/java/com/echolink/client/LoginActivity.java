package com.echolink.client;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * JWT 추출
 *
 * WebViewClient를 사용하여 웹 페이지의 URL 변경을 감지하고,
 * 우리가 원하는 특정 URL(.../login/success?token=...)이 로드될 때 토큰을 추출
 *
 * @author ESH
 */
public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        WebView webView = findViewById(R.id.login_web_view);
        webView.getSettings().setJavaScriptEnabled(true); // 자바스크립트 활성화

        // MainActivity로부터 전달받은 URL
        String url = getIntent().getStringExtra("url");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // [핵심] URL이 성공 리디렉션 주소를 포함하는지 확인
                if (url.startsWith("http://localhost:8080/login/success")) {
                    Uri uri = Uri.parse(url);
                    String token = uri.getQueryParameter("token"); // URL에서 "token" 파라미터 값 추출

                    if (token != null && !token.isEmpty()) {
                        // 추출한 토큰을 SharedPreferences에 저장
                        saveToken(token);
                        Toast.makeText(LoginActivity.this, "로그인 성공!", Toast.LENGTH_SHORT).show();

                        // 로그인 성공 후 현재 액티비티 종료
                        finish();
                        return true; // WebView가 이 URL로 이동하는 것을 막음
                    }
                }
                // 그 외의 URL은 WebView가 직접 처리하도록 함
                view.loadUrl(url);
                return true;
            }
        });

        // 초기 URL 로드
        if (url != null) {
            webView.loadUrl(url);
        }
    }

    private void saveToken(String token) {
        SharedPreferences prefs = getSharedPreferences("EchoLinkPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("jwt_token", token);
        editor.apply();
    }
}