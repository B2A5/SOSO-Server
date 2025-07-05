package com.example.soso.kakao.dto;

import com.example.soso.users.domain.entity.Roles;

public record KakaoUserProfileDto (
        String username,
        String email,
        String nicName,
        String profileImageUrl
){
}
