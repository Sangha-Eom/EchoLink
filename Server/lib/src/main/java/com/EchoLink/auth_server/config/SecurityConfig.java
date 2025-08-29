package com.EchoLink.auth_server.config;

// 소설 로그인
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// 자체 로그인 API
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import static org.springframework.security.config.Customizer.withDefaults;

import com.EchoLink.auth_server.security.JwtAuthenticationFilter;
import com.EchoLink.auth_server.util.JwtUtil;

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
	
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
	
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF 보호 비활성화 (API 서버는 일반적으로 비활성화)
            .csrf(csrf -> csrf.disable())
            // 세션을 사용하지 않고 JWT를 사용하므로 STATELESS로 설정
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 요청별 접근 권한 설정
            .authorizeHttpRequests(auth -> auth
                // 회원가입, 로그인 API는 누구나 접근 가능하도록 허용
                .requestMatchers("/api/auth/**").permitAll()
                // 기기 관련 API도 모두 허용 (이후 단계에서 JWT 인증 추가 예정)
                .requestMatchers("/api/devices/**").permitAll() 
                // 그 외 모든 요청은 인증 필요
                .anyRequest().authenticated()
            );
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