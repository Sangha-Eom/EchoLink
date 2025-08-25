package com.EchoLink.auth_server.config;

import com.EchoLink.auth_server.security.JwtAuthenticationFilter;
import com.EchoLink.auth_server.util.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 인증 서버 보안(사용자 인증) 관리 클래스
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	private final OAuth2SuccessHandler oAuth2SuccessHandler;	// 토큰 발급 성공 핸들러
	private final JwtUtil jwtUtil;	// JWT 토큰 처리

	/**
	 * 생성자
	 * @param oAuth2SuccessHandler
	 * @param jwtUtil
	 */
	public SecurityConfig(OAuth2SuccessHandler oAuth2SuccessHandler, JwtUtil jwtUtil) {
		this.oAuth2SuccessHandler = oAuth2SuccessHandler;
		this.jwtUtil = jwtUtil;
	}

	/**
	 * 
	 * 
	 * - /api/devices/**는 인증 없이 허용(기기 등록/폴링용)
	 * - /api/**는 JWT 필요 (필터가 인증 컨텍스트 세팅)
	 * - OAuth2 Login 성공 시 위의 OAuth2SuccessHandler 사용
	 * - CSRF는 브라우저 폼이 아니라 API 위주이므로 /api/**는 제외
	 * @param http
	 * @return
	 * @throws Exception
	 */
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
		// 브라우저 폼이 아닌 API 호출 중심이므로 /api/**는 CSRF 제외
		.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))

		.authorizeHttpRequests(auth -> auth
				.requestMatchers("/", "/login", "/login/success", "/oauth2/**").permitAll()
				// 기기 등록/폴링은 인증 없이 접근 가능 (서버 초기 등록 흐름)
				.requestMatchers("/api/devices/**").permitAll()
				// 그 외 API는 인증 필요 (JWT 또는 세션)
				.requestMatchers("/api/**").authenticated()
				// 나머지는 자유롭게 조정
				.anyRequest().authenticated()
				)

		// 구글 OAuth2 로그인 (성공 시 JSON으로 token/email 반환)
		.oauth2Login(oauth2 -> oauth2.successHandler(oAuth2SuccessHandler))

		// JWT 필터를 UsernamePasswordAuthenticationFilter 전에 배치
		.addFilterBefore(new JwtAuthenticationFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class);

		return http.build();

	}
}