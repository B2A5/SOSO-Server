package com.example.soso.kakao.service;

import com.example.soso.jwt.JwtTokenDto;
import com.example.soso.kakao.controller.KakaoApiClient;
import com.example.soso.kakao.controller.KakaoAuthClient;
import com.example.soso.kakao.dto.KakaoTokenResponse;
import com.example.soso.kakao.dto.KakaoUserProfileDto;
import com.example.soso.kakao.dto.KakaoUserResponse;
import com.example.soso.kakao.mapper.KakaoMapper;
import com.example.soso.users.domain.entity.Users;
import com.example.soso.users.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KakaoServiceImpl implements KakaoService {


    private final KakaoAuthClient kakaoAuthClient;
    private final KakaoApiClient kakaoApiClient;
    private final UsersRepository userRepository;
//    private final JwtProvider jwtProvider;

    @Value("${oauth.kakao.client-id}")
    private String clientId;

    @Value("${oauth.kakao.redirect-uri}")
    private String redirectUri;

    @Override
    public JwtTokenDto login(String code, String codeVerifier) {

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

        // 4. 회원 조회 또는 신규 생성
        Users user = userRepository.findByEmail(profile.email())
                .orElseGet(() -> userRepository.save(
                        Users.builder()
                                .username(profile.username())
                                .email(profile.email())
                                .nickName(profile.nicName())
                                .profileImageUrl(profile.profileImageUrl())
                                .build()
                ));

        return null;
    }
}
