package com.EchoLink.auth_server.security;

import com.EchoLink.auth_server.service.UserService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

/**
 * 사용자 정보 등록 필터 클래스
 * 
 * HTTP 요청 헤더에서 토큰 추출 후 유효성 검사
 * 인증 성공 시 Spring Security 컨텍스트에 사용자 정보 등록
 * 
 * @author ESH
 */
@Component
public class FirebaseTokenFilter extends OncePerRequestFilter {

    private final UserService userService;

    public FirebaseTokenFilter(UserService userService) {
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String idToken = header.substring(7); // "Bearer " 제거

        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);

            // 1. 사용자 정보 처리 (첫 로그인 시 Firestore에 저장)
            userService.processUserLogin(decodedToken);

            // 2. Spring Security 인증 컨텍스트 설정
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    decodedToken.getEmail(), null, new ArrayList<>());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            // 토큰 검증 실패
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid or expired Firebase token.");
            return;
        }

        filterChain.doFilter(request, response);
    }
}