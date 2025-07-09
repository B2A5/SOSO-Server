package com.example.soso.kakao.service;

import com.example.soso.kakao.controller.KakaoApiClient;
import com.example.soso.kakao.controller.KakaoAuthClient;
import com.example.soso.kakao.dto.KakaoTokenResponse;
import com.example.soso.kakao.dto.KakaoUserProfileDto;
import com.example.soso.kakao.dto.KakaoUserResponse;
import com.example.soso.kakao.mapper.KakaoMapper;
import com.example.soso.users.domain.dto.SignupSession;
import com.example.soso.users.domain.entity.SignupStep;
import com.example.soso.users.repository.UsersRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KakaoServiceImpl implements KakaoService {

    private final KakaoAuthClient kakaoAuthClient;
    private final KakaoApiClient kakaoApiClient;
    private final UsersRepository userRepository;

    @Value("${oauth.kakao.client-id}")
    private String clientId;

    @Value("${oauth.kakao.redirect-uri}")
    private String redirectUri;

    @Override
    public void login(String code, String codeVerifier, HttpSession session) {
        // 1. 카카오 토큰 요청
        KakaoTokenResponse tokenResponse = kakaoAuthClient.getToken(
                "authorization_code",
                clientId,
                redirectUri,
                code,
                codeVerifier
        );

        // 2. 사용자 정보 요청
        KakaoUserResponse kakaoUser = kakaoApiClient.getUserInfo("Bearer " + tokenResponse.accessToken());

        // 3. 사용자 정보를 Dto로 변환
        KakaoUserProfileDto profile = KakaoMapper.from(kakaoUser);

        // 4. 기존 유저인지 확인
        boolean exists = userRepository.existsByEmail(profile.email());
        if (exists) {
            throw new IllegalStateException("이미 가입된 이메일입니다. (소셜 로그인)");
        }

        // 5. 세션에 SignupSession 저장
        SignupSession signup = new SignupSession();
        signup.setEmail(profile.email());
        signup.setProfileImageUrl(profile.profileImageUrl());
        session.setAttribute("signup", signup);
    }
}
