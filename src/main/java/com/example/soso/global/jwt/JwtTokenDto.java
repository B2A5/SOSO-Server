package com.example.soso.global.jwt;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "JWT 토큰 DTO")
public record JwtTokenDto(

        @Schema(description = "Access Token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String jwtAccessToken

) {}