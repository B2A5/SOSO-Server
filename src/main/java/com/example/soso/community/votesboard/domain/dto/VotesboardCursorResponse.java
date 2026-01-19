package com.example.soso.community.votesboard.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 투표 게시판 커서 기반 목록 조회 응답
 */
@Schema(description = "투표 게시판 커서 기반 목록 조회 응답")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VotesboardCursorResponse {

    @Schema(description = "투표 게시글 목록", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<VotesboardSummary> posts;

    @Schema(description = "다음 페이지 존재 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean hasNext;

    @Schema(description = "다음 페이지를 위한 커서 값", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String nextCursor;

    @Schema(description = "현재 페이지 크기", example = "20", requiredMode = Schema.RequiredMode.REQUIRED)
    private int size;

    @Schema(description = "총 게시글 수", example = "150", requiredMode = Schema.RequiredMode.REQUIRED)
    private long totalCount;

    @Schema(description = "요청한 사용자가 인증되었는지 여부 (액세스 토큰 제공 여부)", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("isAuthorized")
    private boolean isAuthorized;
}
