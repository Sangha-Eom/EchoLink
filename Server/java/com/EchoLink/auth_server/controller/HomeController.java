package com.EchoLink.auth_server.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    // 사용자가 웹 브라우저에서 루트 URL("/")로 접속하면,
    @GetMapping("/")
    public String home() {
        // "home.html" 파일을 찾아서 보여주라는 의미
        return "home";
    }
}