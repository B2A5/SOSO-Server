package com.example.soso.community.common.post.domain.dto;


import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "커서 기반 페이지네이션 정보")
public record CursorDto(

        @Schema(description = "다음 페이지 존재 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean hasNext,

        @Schema(description = "정렬 기준 필드 값 (createdAt, likeCount, commentCount 중 하나). 마지막 페이지인 경우 null", example = "2025-07-28T15:30:00", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String cursor,

        @Schema(description = "보조 커서: 마지막 게시글의 ID. 마지막 페이지인 경우 null", example = "150", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Long idAfter

) {}
