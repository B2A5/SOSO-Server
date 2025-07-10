package com.example.soso.kakao.service;

import com.example.soso.kakao.dto.KakaoLoginResult;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public interface KakaoService {

   KakaoLoginResult login(String code, String codeVerifier, HttpSession session, HttpServletResponse
           response);
}
