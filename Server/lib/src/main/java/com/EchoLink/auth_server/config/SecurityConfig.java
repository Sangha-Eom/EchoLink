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
	
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())	// API 서버는 CSRF 보호가 필요 없으므로 비활성화합니다.
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // JWT 토큰 기반의 인증을 사용하므로 세션을 STATELESS로 설정합니다.
            .authorizeHttpRequests(auth -> auth 	
            	.requestMatchers("/api/firebase/signin").permitAll()
                .requestMatchers("/api/devices/**").authenticated()	// '/api/devices/**' 경로의 모든 요청은 인증을 요구합니다.
                .anyRequest().authenticated()	// 그 외 모든 요청도 인증을 요구합니다.
            )
            // 우리가 직접 만든 FirebaseTokenFilter를 Spring Security의 필터 체인에 추가합니다.
            .addFilterBefore(firebaseTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    
    
    // ------- 소설 로그인 ------- 추후 재구성
//	/**
//	 * 
//	 * 
//	 * - /api/devices/**는 인증 없이 허용(기기 등록/폴링용)
//	 * - /api/**는 JWT 필요 (필터가 인증 컨텍스트 세팅)
//	 * - OAuth2 Login 성공 시 위의 OAuth2SuccessHandler 사용
//	 * - CSRF는 브라우저 폼이 아니라 API 위주이므로 /api/**는 제외
//	 * @param http
//	 * @return
//	 * @throws Exception
//	 */
//	@Bean
//	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//		http
//		// 브라우저 폼이 아닌 API 호출 중심이므로 /api/**는 CSRF 제외
//		.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
//
//		.authorizeHttpRequests(auth -> auth
//				.requestMatchers("/", "/login", "/login/success", "/oauth2/**").permitAll()
//				// 기기 등록/폴링은 인증 없이 접근 가능 (서버 초기 등록 흐름)
//				.requestMatchers("/api/devices/**").permitAll()
//				// 그 외 API는 인증 필요 (JWT 또는 세션)
//				.requestMatchers("/api/**").authenticated()
//				// 나머지는 자유롭게 조정
//				.anyRequest().authenticated()
//				)
//
//		// 구글 OAuth2 로그인 (성공 시 JSON으로 token/email 반환)
//		.oauth2Login(oauth2 -> oauth2.successHandler(oAuth2SuccessHandler))
//
//		// JWT 필터를 UsernamePasswordAuthenticationFilter 전에 배치
//		.addFilterBefore(new JwtAuthenticationFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class);
//
//		return http.build();
//
//	}
}