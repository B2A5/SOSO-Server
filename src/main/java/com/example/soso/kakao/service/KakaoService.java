package com.example.soso.kakao.service;

import com.example.soso.jwt.JwtTokenDto;
import jakarta.servlet.http.HttpSession;

public interface KakaoService {

   void login(String code, String codeVerifier, HttpSession session);
}
