package com.example.soso.community.votesboard.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 투표 게시판 게시글 생성 응답
 */
@Getter
@AllArgsConstructor
@Schema(description = "투표 게시판 게시글 생성 응답")
public class VoteboardCreateResponse {

    @Schema(description = "생성된 투표 게시글 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long postId;
}
