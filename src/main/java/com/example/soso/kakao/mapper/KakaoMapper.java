package com.example.soso.kakao.mapper;

import com.example.soso.kakao.dto.KakaoUserProfileDto;
import com.example.soso.kakao.dto.KakaoUserResponse;
import com.example.soso.users.domain.entity.Roles;

public class KakaoMapper {

    public static KakaoUserProfileDto from(KakaoUserResponse kakaoUser) {
        return new KakaoUserProfileDto(
                "kakao_" + kakaoUser.id(),
                kakaoUser.kakaoAccount().email(),
                kakaoUser.kakaoAccount().profile().nickname(),
                kakaoUser.kakaoAccount().profile().profileImageUrl()
        );
    }
}
