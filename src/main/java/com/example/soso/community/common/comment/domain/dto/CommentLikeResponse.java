package com.example.soso.community.common.comment.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "댓글 좋아요 응답 DTO")
public record CommentLikeResponse(

        @Schema(description = "좋아요 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean liked,

        @Schema(description = "현재 좋아요 수", example = "12", requiredMode = Schema.RequiredMode.REQUIRED)
        long likeCount

) {}
