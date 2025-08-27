package com.EchoLink.auth_server.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model; // Model 임포트 추가
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam; // RequestParam 임포트 추가

@Controller
public class HomeController {

    // 로그인 웹 접속 시 home.html로 이동
    @GetMapping("/")
    public String home() {
        return "home";
    }

    /**
     * 로그인 성공 후 리디렉션되는 URL을 처리하는 메소드
     * 
     * @param token URL 파라미터로 전달된 JWT
     * @param model 템플릿에 데이터를 전달하기 위한 객체
     * @return login-success.html 템플릿
     */
    @GetMapping("/login/success")
    public String loginSuccess(@RequestParam("token") String token, Model model) {
        // URL의 'token' 파라미터를 받아서 'token'이라는 이름으로 모델에 추가
        model.addAttribute("token", token);
        // login-success.html 템플릿을 사용자에게 보여줌
        return "login-success";
    }
}