package com.example.soso.community.freeboard.post.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "자유게시판 정렬 기준")
public enum FreeboardSortType {
    @Schema(description = "최신순 (기본값)")
    LATEST,

    @Schema(description = "좋아요순")
    LIKE,

    @Schema(description = "댓글순")
    COMMENT,

    @Schema(description = "조회순")
    VIEW
}