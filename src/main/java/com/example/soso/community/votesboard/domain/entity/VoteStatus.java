package com.example.soso.community.votesboard.domain.entity;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 투표 상태
 */
@Schema(description = "투표 상태")
public enum VoteStatus {

    @Schema(description = "진행 중")
    IN_PROGRESS,

    @Schema(description = "완료")
    COMPLETED,

    @Schema(description = "삭제됨")
    DELETED
}
