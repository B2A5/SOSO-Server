package com.example.soso.users.controller;

import com.example.soso.global.config.CookieUtil;
import com.example.soso.jwt.JwtProperties;
import com.example.soso.jwt.JwtProvider;
import com.example.soso.jwt.JwtTokenDto;
import com.example.soso.jwt.RefreshTokenRedisService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final JwtProvider jwtProvider;
    private final RefreshTokenRedisService refreshTokenService;
    private final JwtProperties jwtProperties;

    public JwtTokenDto refreshAccessToken(String refreshToken, HttpServletResponse response) {

        if (!jwtProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }

        String userId = jwtProvider.getSubject(refreshToken);
        String stored = refreshTokenService.get(userId);

        if (!refreshToken.equals(stored)) {
            throw new IllegalArgumentException("재사용되었거나 탈취된 토큰입니다.");
        }

        // RTR: refreshToken 교체
        String newRefreshToken = jwtProvider.generateRefreshToken(userId);
        refreshTokenService.save(userId, newRefreshToken, jwtProperties.getRefreshTokenValidityInMs());

        // 쿠키에 다시 설정
        CookieUtil.addRefreshTokenCookie(response, newRefreshToken, jwtProperties.getRefreshTokenValidityInMs());

        // accessToken 발급
        String newAccessToken = jwtProvider.generateAccessToken(userId);

        return new JwtTokenDto(newAccessToken);
    }
}
