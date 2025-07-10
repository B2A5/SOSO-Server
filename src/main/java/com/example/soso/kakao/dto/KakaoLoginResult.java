package com.example.soso.kakao.dto;

public record KakaoLoginResult(
        boolean isNewUser,
        String accessToken
) {
}

