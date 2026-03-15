package com.example.soso.community.pollboard.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

/**
 * 투표 옵션 응답 DTO
 */
@Getter
@Builder
@Schema(description = "투표 옵션 정보")
public class PollOptionResponse {

    @Schema(description = "투표 옵션 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "투표 옵션 내용", example = "찬성", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @Schema(description = "옵션 순서", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    private int sequence;

    @Schema(description = "이 옵션에 투표한 사람 수", example = "42", requiredMode = Schema.RequiredMode.REQUIRED)
    private int voteCount;

    @Schema(description = "투표 비율 (%)", example = "35.5", requiredMode = Schema.RequiredMode.REQUIRED)
    private double percentage;
}
