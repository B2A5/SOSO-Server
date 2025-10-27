package com.example.soso.community.voteboard.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    @Schema(description = "선택한 투표 옵션 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long voteOptionId;
}
