package com.example.soso.community.common.comment.domain.dto;

import com.example.soso.community.common.post.domain.dto.UserSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "댓글 응답 DTO")
public record PostCommentResponse(

        @Schema(description = "댓글 ID", example = "301", requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,

        @Schema(description = "댓글 내용", example = "저도 봤어요! 정말 유익했어요.", requiredMode = Schema.RequiredMode.REQUIRED)
        String content,

        @Schema(description = "해당 댓글에 좋아요 수", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
        int likeCount,

        @Schema(description = "작성일시", example = "2025-07-26T12:34:56", requiredMode = Schema.RequiredMode.REQUIRED)
        String createdAt,

        @Schema(description = "작성자 정보", requiredMode = Schema.RequiredMode.REQUIRED)
        UserSummaryResponse user
) {}
