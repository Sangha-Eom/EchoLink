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
    
    /**
     * 구글 로그인 성공 시 JSON으로 email과 token 내려줌
     * 이후 모바일/데스크톱이 OAuth 로그인 완료 시 이 JSON을 받아 token을 로컬에 저장.
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, 
    		HttpServletResponse response, 
    		Authentication authentication) throws IOException, ServletException {
    	
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email"); // Google로부터 받은 이메일 정보
        String token = jwtUtil.generateToken(email); 	// 이메일로 JWT 생성

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String body = "{ \"token\": \"" + token + "\", \"email\": \"" + email + "\" }";
        response.getWriter().write(body);
        response.getWriter().flush();

    }
}