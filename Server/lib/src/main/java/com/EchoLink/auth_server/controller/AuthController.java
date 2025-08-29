package com.EchoLink.auth_server.controller;

import com.EchoLink.auth_server.dto.AuthRequest;
import com.EchoLink.auth_server.model.User;
import com.EchoLink.auth_server.repository.UserRepository;
import com.EchoLink.auth_server.util.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 사용자의 회원가입과 로그인 요청을 받아 처리하는 API 컨트롤러
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    
    /**
     * 생성자
     * @param userRepository 유저 인터페이스(조회, 저장, 삭제)
     * @param passwordEncoder 비번 암호화
     * @param jwtUtil JWT토큰 생성 및 검증 
     */
    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }
    
    
    /**
     * 회원가입
     * 
     * @param authRequest
     * @return
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody AuthRequest authRequest) {
        if (userRepository.findByEmail(authRequest.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 존재하는 이메일입니다.");
        }
        // 비밀번호를 암호화하여 저장
        User user = new User(authRequest.getEmail(), passwordEncoder.encode(authRequest.getPassword()));
        userRepository.save(user);
        return ResponseEntity.ok("회원가입이 완료되었습니다.");
    }
    
    /**
     * 로그인
     * 
     * @param authRequest
     * @return
     */
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody AuthRequest authRequest) {
        return userRepository.findByEmail(authRequest.getEmail())
            .map(user -> {
                if (passwordEncoder.matches(authRequest.getPassword(), user.getPassword())) {
                    String token = jwtUtil.generateToken(user.getEmail());
                    return ResponseEntity.ok(Map.of("token", token));
                }
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("비밀번호가 일치하지 않습니다.");
            })
            .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("사용자를 찾을 수 없습니다."));
    }
    
}