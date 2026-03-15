package com.example.soso.community.pollboard.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 투표 참여 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "투표 참여 요청")
public class VoteRequest {

    @NotNull(message = "투표 옵션 ID는 필수입니다.")
    @NotEmpty(message = "최소 하나의 옵션을 선택해야 합니다.")
    @Schema(
            description = "선택한 투표 옵션 ID 목록 (단일 선택: 1개, 중복 선택: 최대 n-1개)",
            example = "[1, 2]",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private List<Long> voteOptionIds;
}
