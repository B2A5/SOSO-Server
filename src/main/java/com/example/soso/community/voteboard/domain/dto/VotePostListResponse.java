package com.example.soso.community.voteboard.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 투표 게시글 목록 응답 DTO (커서 기반 페이지네이션)
 */
@Getter
@Builder
@Schema(description = "투표 게시글 목록 응답 (커서 기반 페이지네이션)")
public class VotePostListResponse {

    @Schema(description = "투표 게시글 목록", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<VotePostSummaryResponse> posts;

    @Schema(description = "다음 커서 (다음 페이지 조회용, 없으면 null)", example = "42")
    private Long nextCursor;

    @Schema(description = "다음 페이지 존재 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean hasNext;

    @Schema(description = "현재 페이지 게시글 수", example = "20", requiredMode = Schema.RequiredMode.REQUIRED)
    private int size;
}
