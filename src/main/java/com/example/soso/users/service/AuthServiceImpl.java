package com.example.soso.users.service;

import com.example.soso.global.config.CookieUtil;
import com.example.soso.global.exception.util.InvalidTokenException;
import com.example.soso.jwt.JwtProperties;
import com.example.soso.jwt.JwtProvider;
import com.example.soso.jwt.JwtTokenDto;
import com.example.soso.jwt.RefreshTokenRedisService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.example.soso.global.exception.domain.TokenErrorCode.*;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final JwtProvider jwtProvider;
    private final RefreshTokenRedisService refreshTokenService;
    private final JwtProperties jwtProperties;

    @Override
    public JwtTokenDto refreshAccessToken(String refreshToken, HttpServletResponse response) {
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new InvalidTokenException(INVALID_TOKEN);
        }

        String userId = jwtProvider.getSubject(refreshToken);
        String storedToken = refreshTokenService.get(userId);

        if (!refreshToken.equals(storedToken)) {
            throw new InvalidTokenException(REFRESH_TOKEN_NOT_FOUND);
        }

        // 1. RTR: refreshToken 교체 및 재저장
        String newRefreshToken = jwtProvider.generateRefreshToken(userId);
        refreshTokenService.save(userId, newRefreshToken, jwtProperties.getRefreshTokenValidityInMs());

        // 2. 새 refreshToken 쿠키에 저장
        CookieUtil.addRefreshTokenCookie(response, newRefreshToken, jwtProperties.getRefreshTokenValidityInMs());

        // 3. 새 accessToken 발급
        String newAccessToken = jwtProvider.generateAccessToken(userId);

        return new JwtTokenDto(newAccessToken);
    }
}
