package com.example.soso.users.service;

import com.example.soso.global.config.CookieUtil;
import com.example.soso.global.exception.util.InvalidTokenException;
import com.example.soso.global.jwt.JwtProperties;
import com.example.soso.global.jwt.JwtProvider;
import com.example.soso.global.jwt.JwtTokenDto;
import com.example.soso.global.redis.RefreshTokenRedisRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.example.soso.global.exception.domain.TokenErrorCode.*;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final JwtProvider jwtProvider;
    private final RefreshTokenRedisRepository refreshTokenService;
    private final JwtProperties jwtProperties;

    @Override
    public JwtTokenDto refreshAccessToken(String refreshToken, HttpServletResponse response) {
        // 1. 토큰 유효성 검사 (exp, 서명 등)
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new InvalidTokenException(INVALID_TOKEN);
        }

        // 2. Redis에서 refreshToken으로 userId 조회
        String userId = refreshTokenService.getUserIdByRefreshToken(refreshToken);
        if (userId == null) {
            throw new InvalidTokenException(REFRESH_TOKEN_NOT_FOUND);
        }

        // 3. RTR 적용: 기존 토큰 제거 + 새 토큰 발급 및 저장
        refreshTokenService.delete(refreshToken); // 기존 토큰 무효화

        String newRefreshToken = jwtProvider.generateRefreshToken(); // userId 없음
        refreshTokenService.save(newRefreshToken, userId, jwtProperties.getRefreshTokenValidityInMs());

        // 4. 새 accessToken 발급
        String newAccessToken = jwtProvider.generateAccessToken(userId);

        // 5. 쿠키에 새 토큰 저장 (SSR 지원)
        CookieUtil.addAccessTokenCookie(response, newAccessToken, jwtProperties.getAccessTokenValidityInMs());
        CookieUtil.addRefreshTokenCookie(response, newRefreshToken, jwtProperties.getRefreshTokenValidityInMs());

        // Body에도 accessToken 포함 (기존 호환성 유지)
        return new JwtTokenDto(newAccessToken);
    }

    @Override
    public void logout(String refreshToken, HttpServletResponse response) {
        // 1. Redis에서 Refresh Token 삭제 (즉시 무효화)
        refreshTokenService.delete(refreshToken);

        // 2. 쿠키 삭제 (AccessToken, RefreshToken 모두 삭제)
        CookieUtil.deleteAccessTokenCookie(response);
        CookieUtil.deleteRefreshTokenCookie(response);
    }

}
