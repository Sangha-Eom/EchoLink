package com.EchoLink.auth_server.config;

import com.EchoLink.auth_server.util.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

import com.EchoLink.auth_server.util.JwtUtil;

/**
 * 구글 사용자 인증
 * 
 * https://console.cloud.google.com/apis/credentials?hl=ko&inv=1&invt=Ab5uWw&project=encoded-rider-413309
 */
@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    
    /**
     * 생성자
     * JwiUtil 인스턴스 생성
     */
    public OAuth2SuccessHandler(JwtUtil jwtUtil) {
    	this.jwtUtil = jwtUtil;
	}

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email"); // Google로부터 받은 이메일 정보

        String token = jwtUtil.generateToken(email); 	// 이메일로 JWT 생성

        // TODO: 향후 이 URL을 안드로이드 앱에서 받을 수 있는 커스텀 스킴 URL로 변경
        String targetUrl = "http://localhost:8080/login/success?token=" + token;

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}