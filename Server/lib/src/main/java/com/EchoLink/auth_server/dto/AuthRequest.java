package com.EchoLink.auth_server.dto;

/**
 * 로그인/회원가입 요청 시 사용자의 정보를 담는 클래스
 * 
 * 이메일, 비밀번호
 */
public class AuthRequest {
    private String email;
    private String password;
    
    // Getters and Setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}