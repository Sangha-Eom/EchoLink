package com.EchoLink.auth_server.controller;

import com.EchoLink.auth_server.service.DeviceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 기기 연결 API 컨트롤러
 * 
 * 기기 코드 관리(DeviceService) 호출할 수 있는 URL 생성.
 * @author ESH
 */
@Controller
public class DeviceController {

    private final DeviceService deviceService;
    
    /**
     * 생성자
     * @param deviceService
     */
    public DeviceController(DeviceService deviceService) {
		this.deviceService = deviceService;
	}

    // --- API for Desktop Server ---
    @PostMapping("/api/devices/register")
    @ResponseBody // JSON 형태로 응답
    public ResponseEntity<Map<String, String>> registerDevice() {
        String deviceCode = deviceService.registerDevice();
        return ResponseEntity.ok(Map.of("deviceCode", deviceCode));
    }

    @GetMapping("/api/devices/poll/{deviceCode}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> pollDevice(@PathVariable String deviceCode) {
        String token = deviceService.pollDeviceStatus(deviceCode);
        if (token != null) {
            return ResponseEntity.ok(Map.of("token", token));
        }
        return ResponseEntity.noContent().build(); // 아직 인증 안됨 (204 No Content)
    }

    // --- Page for User ---
    @GetMapping("/link")
    public String showLinkPage() {
        return "link"; // link.html 템플릿 보여주기
    }

    @PostMapping("/link")
    public String processLink(@RequestParam String deviceCode, @AuthenticationPrincipal OAuth2User oauth2User, Model model) {
        String email = oauth2User.getAttribute("email");
        boolean success = deviceService.linkDevice(deviceCode, email);
        if (success) {
            model.addAttribute("message", "기기가 성공적으로 연결되었습니다!");
        } else {
            model.addAttribute("message", "유효하지 않은 코드이거나 이미 사용된 코드입니다.");
        }
        return "link-result"; // link-result.html 템플릿 보여주기
    }
}