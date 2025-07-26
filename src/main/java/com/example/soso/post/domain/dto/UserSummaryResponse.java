package com.example.soso.post.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "작성자 요약 정보 DTO")
public record UserSummaryResponse(

        @Schema(description = "닉네임", example = "소소한개발자")
        String nickname,

        @Schema(description = "지역", example = "서울특별시 강남구")
        String location,

        @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
        String profileImageUrl

) {}
