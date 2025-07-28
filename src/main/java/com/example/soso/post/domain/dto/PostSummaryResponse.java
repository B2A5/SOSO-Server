package com.example.soso.post.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "게시글 요약 응답")
public record PostSummaryResponse(

        @Schema(description = "게시글 ID", example = "123")
        Long postId,

        @Schema(description = "게시글 제목", example = "우리 동네 핫플 공유해요")
        String title,

        @Schema(description = "게시글 내용", example = "방금 생긴 카페 너무 좋아요!")
        String content,

        @Schema(description = "카테고리", example = "DAILYANDHOBBY")
        String category,

        @Schema(description = "좋아요 수", example = "10")
        int likeCount,

        @Schema(description = "댓글 수", example = "5")
        int commentCount,

        @Schema(description = "작성 시간", example = "2025-07-28T15:30:00")
        String createdAt,

        @Schema(description = "작성자 정보")
        UserSummaryResponse user

) {}
