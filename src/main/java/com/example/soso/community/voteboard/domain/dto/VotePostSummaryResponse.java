package com.example.soso.community.voteboard.domain.dto;

import com.example.soso.community.voteboard.domain.entity.VoteStatus;
import com.example.soso.community.common.post.domain.dto.UserSummaryResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 투표 게시글 요약 응답 DTO (목록 조회용)
 */
@Getter
@Builder
@Schema(description = "투표 게시글 요약 정보 (목록 조회)")
public class VotePostSummaryResponse {

    @Schema(description = "게시글 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "게시글 제목", example = "우리 동네 카페 선호도 조사", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "작성자 정보", requiredMode = Schema.RequiredMode.REQUIRED)
    private UserSummaryResponse author;

    @Schema(description = "조회수", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
    private int viewCount;

    @Schema(description = "댓글 수", example = "45", requiredMode = Schema.RequiredMode.REQUIRED)
    private long commentCount;

    @Schema(description = "총 투표 참여자 수", example = "89", requiredMode = Schema.RequiredMode.REQUIRED)
    private int totalVotes;

    @Schema(description = "투표 상태 (IN_PROGRESS: 진행중, COMPLETED: 완료)", example = "IN_PROGRESS", requiredMode = Schema.RequiredMode.REQUIRED)
    private VoteStatus voteStatus;

    @Schema(description = "투표 마감 시간", example = "2024-12-31T23:59:59", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime endTime;

    @Schema(description = "재투표 허용 여부 (투표 후 변경 가능 여부)", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean allowRevote;

    @Schema(description = "중복 선택 허용 여부 (여러 옵션 동시 선택 가능 여부)", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean allowMultipleChoice;

    @Schema(description = "투표 옵션 목록 (미리보기, 최대 3개)", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<VoteOptionResponse> voteOptions;

    @Schema(description = "좋아요 수", example = "23", requiredMode = Schema.RequiredMode.REQUIRED)
    private long likeCount;

    @Schema(description = "현재 사용자의 좋아요 여부 (비로그인 시 false)", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("isLiked")
    private boolean isLiked;

    @Schema(description = "생성일시", example = "2024-01-01T10:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createdDate;

    @Schema(description = "수정일시", example = "2024-01-02T15:30:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime lastModifiedDate;
}
