package com.EchoLink.auth_server.config;

// 데이터베이스(Firebase) 접근
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.EchoLink.auth_server.security.FirebaseTokenFilter;

/**
 * 인증 서버 보안(사용자 인증) 관리 클래스
 * 
 * @author ESH
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
	
	private final FirebaseTokenFilter firebaseTokenFilter;

	/**
	 * 생성자
	 * @param firebaseTokenFilter firebase 초기화
	 */
	public SecurityConfig(FirebaseTokenFilter firebaseTokenFilter) {
		this.firebaseTokenFilter = firebaseTokenFilter;
	}
	
	/**
	 * 사용자 인증 메소드
	 * @param http
	 * @return
	 * @throws Exception
	 */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())	// API 서버는 CSRF 보호가 필요 없으므로 비활성화.
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // JWT 토큰 기반의 인증을 사용하므로 세션을 STATELESS로 설정합니다.
            .authorizeHttpRequests(auth -> auth 	
            	.requestMatchers("/api/firebase/signin").permitAll()	// 로그인 허용
            	.requestMatchers("/api/auth/refresh").permitAll()	// 토큰 재발급 허용
                .requestMatchers("/api/devices/**").authenticated()	// '/api/devices/**' 경로의 모든 요청은 인증을 요구합니다.
                .anyRequest().authenticated()	// 그 외 모든 요청도 인증을 요구합니다.
            )
            // 우리가 직접 만든 FirebaseTokenFilter를 Spring Security의 필터 체인에 추가합니다.
            .addFilterBefore(firebaseTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    
    
}
