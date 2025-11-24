package com.example.soso.kakao.service;

import com.example.soso.global.config.CookieUtil;
import com.example.soso.global.exception.domain.UserErrorCode;
import com.example.soso.global.exception.util.UserAuthException;
import com.example.soso.global.jwt.JwtProperties;
import com.example.soso.global.jwt.JwtProvider;
import com.example.soso.global.redis.RefreshTokenRedisRepository;
import com.example.soso.kakao.dto.KakaoLoginResponse;
import com.example.soso.users.domain.dto.UserMapper;
import com.example.soso.users.domain.dto.UserResponse;
import com.example.soso.users.domain.entity.Users;
import com.example.soso.users.repository.UsersRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserLoginService {

    private final UsersRepository userRepository;
    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;
    private final RefreshTokenRedisRepository refreshTokenService;
    private final UserMapper userMapper;
    private final CookieUtil cookieUtil;

    public boolean isRegistered(String email) {
        return userRepository.existsByEmail(email);
    }

    public KakaoLoginResponse login(String email, HttpServletResponse response) {
        Users user = userRepository.findByEmailWithInterests(email)
                .orElseThrow(() -> new UserAuthException(UserErrorCode.USER_NOT_FOUND));

        String accessToken = jwtProvider.generateAccessToken(user.getId());
        String refreshToken = jwtProvider.generateRefreshToken();

        // Redis에 Refresh Token 저장
        refreshTokenService.save(refreshToken, user.getId(), jwtProperties.getRefreshTokenValidityInMs());

        // 쿠키에 토큰 설정 (httpOnly=true로 XSS 방어)
        cookieUtil.addAccessTokenCookie(response, accessToken, jwtProperties.getAccessTokenValidityInMs());
        cookieUtil.addRefreshTokenCookie(response, refreshToken, jwtProperties.getRefreshTokenValidityInMs());

        UserResponse userResponse = userMapper.toUserResponse(user);

        // Body에도 accessToken 포함 (모바일 앱 지원 필수)
        // - 웹: 쿠키 자동 관리 (httpOnly=true)
        // - 모바일: Body에서 토큰 추출 후 AsyncStorage/SharedPreferences 저장
        return new KakaoLoginResponse(false, accessToken, userResponse);
    }
}
