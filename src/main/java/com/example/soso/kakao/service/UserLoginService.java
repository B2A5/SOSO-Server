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

    public boolean isRegistered(String email) {
        return userRepository.existsByEmail(email);
    }

    public KakaoLoginResponse login(String email, HttpServletResponse response) {
        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserAuthException(UserErrorCode.USER_NOT_FOUND));

        String accessToken = jwtProvider.generateAccessToken(user.getId());
        String refreshToken = jwtProvider.generateRefreshToken();

        refreshTokenService.save(refreshToken, user.getId(), jwtProperties.getRefreshTokenValidityInMs());
        CookieUtil.addRefreshTokenCookie(response, refreshToken, jwtProperties.getRefreshTokenValidityInMs());

        UserResponse userResponse = UserMapper.toUserResponse(user);

        return new KakaoLoginResponse(false, accessToken, userResponse);
    }
}
