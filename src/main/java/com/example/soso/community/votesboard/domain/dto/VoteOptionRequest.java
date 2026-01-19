package com.example.soso.community.votesboard.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 투표 옵션 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "투표 옵션 생성 요청")
public class VoteOptionRequest {

    @NotBlank(message = "투표 옵션 내용은 필수입니다.")
    @Size(max = 100, message = "투표 옵션은 최대 100자까지 입력 가능합니다.")
    @Schema(description = "투표 옵션 내용", example = "찬성", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;
}
