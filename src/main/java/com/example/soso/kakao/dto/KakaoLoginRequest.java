package com.example.soso.kakao.dto;

public record KakaoLoginRequest(
        String code,
        String codeVerifier,
        String redirectUri,
        String state
) {
}
