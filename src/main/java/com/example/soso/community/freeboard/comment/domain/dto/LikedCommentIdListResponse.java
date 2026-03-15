package com.example.soso.community.freeboard.comment.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "좋아요한 댓글 ID 목록 응답")
public record LikedCommentIdListResponse(

        @Schema(description = "좋아요한 댓글 ID 목록", example = "[1, 2, 3]", requiredMode = Schema.RequiredMode.REQUIRED)
        List<Long> likedCommentIds

) {}
