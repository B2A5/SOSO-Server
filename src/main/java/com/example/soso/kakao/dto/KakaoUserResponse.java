package com.example.soso.kakao.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoUserResponse(
        Long id,

        @JsonProperty("kakao_account")
        KakaoAccount kakaoAccount
) {
    public record KakaoAccount(
            Profile profile,
            String email
    ) {
        public record Profile(
                String nickname,

                @JsonProperty("profile_image_url")
                String profileImageUrl
        ) {}
    }
}
