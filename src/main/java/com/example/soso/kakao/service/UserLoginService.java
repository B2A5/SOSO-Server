package com.example.soso.kakao.service;

import com.example.soso.global.config.CookieUtil;
import com.example.soso.global.exception.domain.UserErrorCode;
import com.example.soso.global.exception.util.UserAuthException;
import com.example.soso.jwt.JwtProperties;
import com.example.soso.jwt.JwtProvider;
import com.example.soso.jwt.RefreshTokenRedisService;
import com.example.soso.kakao.dto.KakaoLoginResult;
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
    private final RefreshTokenRedisService refreshTokenService;

    public boolean isRegistered(String email) {
        return userRepository.existsByEmail(email);
    }

    public KakaoLoginResult login(String email, HttpServletResponse response) {
        String userId = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserAuthException(UserErrorCode.USER_NOT_FOUND))
                .getId();

        String accessToken = jwtProvider.generateAccessToken(userId);
        String refreshToken = jwtProvider.generateRefreshToken();

        refreshTokenService.save(refreshToken, userId, jwtProperties.getRefreshTokenValidityInMs());
        CookieUtil.addRefreshTokenCookie(response, refreshToken, jwtProperties.getRefreshTokenValidityInMs());

        return new KakaoLoginResult(false, accessToken);
    }
}
