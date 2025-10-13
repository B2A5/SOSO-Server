package com.example.soso.kakao.service;

import com.example.soso.kakao.dto.KakaoLoginResponse;
import com.example.soso.kakao.dto.KakaoUserProfileDto;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KakaoServiceImpl implements KakaoService {


    private final KakaoOAuthService kakaoOAuthService;
    private final UserLoginService userLoginService;
    private final SignupSessionService signupSessionService;


    @Override
    public KakaoLoginResponse login(String code, String codeVerifier, String redirectUri, HttpSession session, HttpServletResponse
            response) {
        KakaoUserProfileDto profile = kakaoOAuthService.fetchUserProfile(code, codeVerifier, redirectUri);

        if (userLoginService.isRegistered(profile.email())) {
            return userLoginService.login(profile.email(), response);
        }

        signupSessionService.save(session, profile);
        return new KakaoLoginResponse(true, null, null); // 신규 유저: accessToken과 user 정보 없음
    }
}
