package com.example.soso.community.common.likes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "게시글 좋아요 응답 DTO")
public record PostLikeResponse(

        @Schema(description = "좋아요 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean liked,

        @Schema(description = "해당 게시글의 총 좋아요 수", example = "124", requiredMode = Schema.RequiredMode.REQUIRED)
        long likeCount

) {}
