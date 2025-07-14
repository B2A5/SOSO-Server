package com.example.soso.kakao.service;

import com.example.soso.kakao.dto.KakaoLoginResult;
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
    public KakaoLoginResult login(String code, String codeVerifier, HttpSession session, HttpServletResponse
            response) {
        KakaoUserProfileDto profile = kakaoOAuthService.fetchUserProfile(code, codeVerifier);

        if (userLoginService.isRegistered(profile.email())) {
            return userLoginService.login(profile.email(), response);
        }

        signupSessionService.save(session, profile);
        return new KakaoLoginResult(true, null); // 신규 유저: accessToken은 발급 안 함
    }
}
