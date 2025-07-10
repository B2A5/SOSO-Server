package com.example.soso.kakao.service;

import com.example.soso.global.config.CookieUtil;
import com.example.soso.global.exception.domain.UserErrorCode;
import com.example.soso.global.exception.util.UserAuthException;
import com.example.soso.jwt.JwtProperties;
import com.example.soso.jwt.JwtProvider;
import com.example.soso.jwt.RefreshTokenRedisService;
import com.example.soso.kakao.controller.KakaoApiClient;
import com.example.soso.kakao.controller.KakaoAuthClient;
import com.example.soso.kakao.dto.KakaoLoginResult;
import com.example.soso.kakao.dto.KakaoTokenResponse;
import com.example.soso.kakao.dto.KakaoUserProfileDto;
import com.example.soso.kakao.dto.KakaoUserResponse;
import com.example.soso.kakao.mapper.KakaoMapper;
import com.example.soso.users.domain.dto.SignupSession;
import com.example.soso.users.repository.UsersRepository;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
