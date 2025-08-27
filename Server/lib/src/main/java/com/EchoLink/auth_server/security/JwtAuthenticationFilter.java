package com.EchoLink.auth_server.security;

import com.EchoLink.auth_server.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authorization: Bearer <JWT> 를 파싱해서 SecurityContext에 인증 정보를 적재
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                // JwtUtil에서 유효성 검증 + 이메일(또는 subject) 추출
                if (jwtUtil.validateToken(token)) {
                    String email = jwtUtil.extractEmail(token); // ← 필요 시 JwtUtil에 구현

                    // 간단하게 ROLE_USER 부여. 필요하다면 roles 클레임에서 읽어오세요.
                    AbstractAuthenticationToken authentication =
                            new AbstractAuthenticationToken(List.of(new SimpleGrantedAuthority("ROLE_USER"))) {
                                @Override public Object getCredentials() { return token; }
                                @Override public Object getPrincipal() { return email; }
                                @Override public boolean isAuthenticated() { return true; }
                            };

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception ex) {
                // 토큰 불량이면 그냥 통과시키되(= 익명), 보안이 필요한 엔드포인트에서 401 처리됨
                // 로깅 필요 시 여기서 warn 로그 남기세요.
            }
        }

        filterChain.doFilter(request, response);
    }
}
