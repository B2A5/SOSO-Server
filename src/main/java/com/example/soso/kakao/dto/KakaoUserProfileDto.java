package com.example.soso.kakao.dto;

public record KakaoUserProfileDto (
        String username,
        String email,
        String nicName,
        String profileImageUrl
){
}
