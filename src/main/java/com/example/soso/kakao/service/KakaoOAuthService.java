package com.example.soso.kakao.service;

import com.example.soso.kakao.controller.KakaoApiClient;
import com.example.soso.kakao.controller.KakaoAuthClient;
import com.example.soso.kakao.dto.KakaoTokenResponse;
import com.example.soso.kakao.dto.KakaoUserProfileDto;
import com.example.soso.kakao.dto.KakaoUserResponse;
import com.example.soso.kakao.mapper.KakaoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KakaoOAuthService {

    private final KakaoAuthClient kakaoAuthClient;
    private final KakaoApiClient kakaoApiClient;

    @Value("${oauth.kakao.client-id}")
    private String clientId;

    @Value("${oauth.kakao.redirect-uri}")
    private String redirectUri;

    public KakaoUserProfileDto fetchUserProfile(String code, String codeVerifier) {
        KakaoTokenResponse tokenResponse = kakaoAuthClient.getToken(
                "authorization_code", clientId, redirectUri, code, codeVerifier);
        KakaoUserResponse userResponse = kakaoApiClient.getUserInfo("Bearer " + tokenResponse.accessToken());
        return KakaoMapper.from(userResponse);
    }
}
