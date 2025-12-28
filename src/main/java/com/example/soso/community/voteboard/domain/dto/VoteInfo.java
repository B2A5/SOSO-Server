package com.example.soso.community.voteboard.domain.dto;

import com.example.soso.community.voteboard.domain.entity.VoteStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 투표 정보
 */
@Schema(description = "투표 정보")
public record VoteInfo(
    @Schema(description = "현재 사용자가 선택한 옵션 ID 목록 (미투표 시 빈 리스트)", example = "[1, 2]", requiredMode = Schema.RequiredMode.REQUIRED)
    List<Long> selectedOptionIds,

    @Schema(description = "총 투표 참여자 수", example = "89", requiredMode = Schema.RequiredMode.REQUIRED)
    int totalVotes,

    @Schema(description = "투표 상태 (IN_PROGRESS: 진행중, COMPLETED: 완료)", example = "IN_PROGRESS", requiredMode = Schema.RequiredMode.REQUIRED)
    VoteStatus voteStatus,

    @Schema(description = "투표 마감 시간", example = "2024-12-31T23:59:59", requiredMode = Schema.RequiredMode.REQUIRED)
    LocalDateTime endTime,

    @Schema(description = "재투표 허용 여부 (투표 후 변경 가능 여부)", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    boolean allowRevote,

    @Schema(description = "중복 선택 허용 여부 (여러 옵션 동시 선택 가능 여부)", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    boolean allowMultipleChoice
) {}
