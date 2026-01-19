package com.example.soso.community.votesboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "투표게시판 정렬 기준")
public enum VotesboardSortType {
    @Schema(description = "최신순 (기본값)")
    LATEST,

    @Schema(description = "투표순 (투표 인원 많은 순)")
    LIKE,

    @Schema(description = "댓글순")
    COMMENT,

    @Schema(description = "조회순")
    VIEW
}
