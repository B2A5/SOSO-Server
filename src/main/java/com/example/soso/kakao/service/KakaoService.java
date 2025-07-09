package com.example.soso.kakao.service;

import jakarta.servlet.http.HttpSession;

public interface KakaoService {

   void login(String code, String codeVerifier,HttpSession session);
}
