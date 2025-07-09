package com.example.soso.kakao.controller;

import com.example.soso.kakao.dto.KakaoLoginRequest;
import com.example.soso.kakao.service.KakaoService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/kakao")
@RequiredArgsConstructor
public class KakaoAuthController {

    private final KakaoService kakaoService;

    @PostMapping("/login")
    public ResponseEntity<Void> kakaoLogin(
            @RequestBody KakaoLoginRequest request,  HttpSession session
    ) {
        kakaoService.login(request.code(), request.codeVerifier(), session);
        return ResponseEntity.ok().build(); // or 201 Created, or redirect 응답
    }
}
