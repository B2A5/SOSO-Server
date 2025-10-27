package com.example.soso.community.voteboard.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 투표 게시글 ID 응답 DTO
 */
@Getter
@AllArgsConstructor
@Schema(description = "투표 게시글 ID 응답")
public class VotePostIdResponse {

    @Schema(description = "생성된 투표 게시글 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long votesboardId;
}
