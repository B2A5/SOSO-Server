package com.example.soso.kakao.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "카카오 로그인 요청 DTO")
public record KakaoLoginRequest(

        @Schema(description = "인가 코드", example = "abc123xyz")
        String code,

        @Schema(description = "PKCE code_verifier", example = "s3cr3tVer1f13r")
        String codeVerifier,

        @Schema(description = "인가 코드 발급 시 사용한 redirect_uri", example = "https://example.com/oauth/callback")
        String redirectUri,

        @Schema(description = "CSRF 방지용 상태값", example = "xyz987state")
        String state

) {}
