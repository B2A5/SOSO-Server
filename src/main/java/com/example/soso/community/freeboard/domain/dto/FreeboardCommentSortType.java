package com.example.soso.community.freeboard.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "자유게시판 댓글 정렬 기준")
public enum FreeboardCommentSortType {
    @Schema(description = "최신순 (기본값)")
    LATEST,

    @Schema(description = "오래된순")
    OLDEST
}