package com.example.soso.kakao.dto;

import com.example.soso.users.domain.dto.UserResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "카카오 로그인 응답")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record KakaoLoginResponse(

        @Schema(description = "신규 사용자 여부 (true: 회원가입 필요, false: 기존 사용자)", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean isNewUser,

        @Schema(description = "Access Token (기존 사용자인 경우에만 제공)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String accessToken,

        @Schema(description = "사용자 정보 (기존 사용자인 경우에만 제공)")
        UserResponse user

) {}
