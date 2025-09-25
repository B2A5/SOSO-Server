package com.example.soso.community.common.post.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "게시글 정렬 기준")
public enum PostSortType {

    @Schema(description = "최신순 (createdAt DESC)")
    LATEST,

    @Schema(description = "좋아요순 (likeCount DESC)")
    LIKE,

    @Schema(description = "댓글순 (commentCount DESC)")
    COMMENT
}
