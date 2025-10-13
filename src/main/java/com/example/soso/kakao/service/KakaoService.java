package com.example.soso.kakao.service;

import com.example.soso.kakao.dto.KakaoLoginResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public interface KakaoService {

   KakaoLoginResponse login(String code, String codeVerifier, String redirectUri, HttpSession session, HttpServletResponse
           response);
}
