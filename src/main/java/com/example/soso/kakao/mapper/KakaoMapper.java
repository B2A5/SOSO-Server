package com.example.soso.kakao.mapper;

import com.example.soso.kakao.dto.KakaoUserProfileDto;
import com.example.soso.kakao.dto.KakaoUserResponse;

public class KakaoMapper {

    public static KakaoUserProfileDto from(KakaoUserResponse kakaoUser) {
        String id = "kakao_" + kakaoUser.id();
        String email = null;
        String profileImageUrl = null;

        if (kakaoUser.kakaoAccount() != null) {
            email = kakaoUser.kakaoAccount().email();

            var profile = kakaoUser.kakaoAccount().profile();
            if (profile != null) {
                profileImageUrl = profile.profileImageUrl();
            }
        }
        return new KakaoUserProfileDto(id, email, profileImageUrl);
    }
}
