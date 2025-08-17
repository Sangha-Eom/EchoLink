package com.EchoLink.auth_server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                // 루트("/") URL과 로그인 관련 페이지는 누구나 접근 허용
                .requestMatchers("/", "/login").permitAll()
                // 그 외 모든 요청은 반드시 인증(로그인)된 사용자만 접근 가능
                .anyRequest().authenticated()
            )
            // OAuth2 소셜 로그인 기능 활성화 (기본 설정 사용)
            .oauth2Login(withDefaults());

        return http.build();
    }
}