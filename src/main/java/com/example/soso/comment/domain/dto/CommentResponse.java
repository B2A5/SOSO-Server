package com.example.soso.comment.domain.dto;

import com.example.soso.post.domain.dto.UserSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "댓글 응답 DTO")
public record CommentResponse(

        @Schema(description = "댓글 ID", example = "101")
        Long id,

        @Schema(description = "댓글 내용", example = "정말 공감돼요!")
        String content,

        @Schema(description = "댓글 좋아요 수", example = "7")
        int likeCount,

        @Schema(description = "댓글 생성일 (ISO 8601)", example = "2025-07-26T16:45:00")
        String createdAt,

        @Schema(description = "댓글 작성자 정보")
        UserSummaryResponse user

) {}
