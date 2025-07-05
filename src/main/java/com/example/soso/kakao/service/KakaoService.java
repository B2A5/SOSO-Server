package com.example.soso.kakao.service;

import com.example.soso.jwt.JwtTokenDto;

public interface KakaoService {

   JwtTokenDto login(String code, String codeVerifier);
}
