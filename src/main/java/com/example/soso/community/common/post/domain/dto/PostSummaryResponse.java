package com.example.soso.community.common.post.domain.dto;

import com.example.soso.community.common.post.domain.entity.Category;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "게시글 요약 응답")
public record PostSummaryResponse(

        @Schema(description = "게시글 ID", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
        Long postId,

        @Schema(description = "게시글 제목", example = "우리 동네 핫플 공유해요", requiredMode = Schema.RequiredMode.REQUIRED)
        String title,

        @Schema(description = "게시글 내용", example = "방금 생긴 카페 너무 좋아요!", requiredMode = Schema.RequiredMode.REQUIRED)
        String content,

        @Schema(description = "카테고리", example = "restaurant", requiredMode = Schema.RequiredMode.REQUIRED)
        Category category,

        @Schema(description = "좋아요 수", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
        int likeCount,

        @Schema(description = "댓글 수", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
        int commentCount,

        @Schema(description = "조회 수", example = "120", requiredMode = Schema.RequiredMode.REQUIRED)
        int viewCount,

        @Schema(description = "내가 좋아요 누름 게시글", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean likeByPost,

        @Schema(description = "작성 시간", example = "2025-07-28T15:30:00", requiredMode = Schema.RequiredMode.REQUIRED)
        java.time.LocalDateTime createdAt,

        @Schema(description = "작성자 정보", requiredMode = Schema.RequiredMode.REQUIRED)
        UserSummaryResponse user

) {}
