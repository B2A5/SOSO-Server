package com.example.soso.users.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원가입 완료 응답")
public record SignupCompleteResponse(

        @Schema(description = "Access Token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...", requiredMode = Schema.RequiredMode.REQUIRED)
        String accessToken,

        @Schema(description = "사용자 정보", requiredMode = Schema.RequiredMode.REQUIRED)
        UserResponse user

) {}
