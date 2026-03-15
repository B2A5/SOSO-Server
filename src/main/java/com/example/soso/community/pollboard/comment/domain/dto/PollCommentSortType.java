package com.example.soso.community.pollboard.comment.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "댓글 정렬 기준")
public enum PollCommentSortType {
    @Schema(description = "최신순")
    LATEST,

    @Schema(description = "오래된순")
    OLDEST
}
