package com.example.soso.community.voteboard.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 투표 옵션 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "투표 옵션 생성 요청")
public class VoteOptionRequest {

    @NotBlank(message = "투표 옵션 내용은 필수입니다.")
    @Size(max = 100, message = "투표 옵션은 최대 100자까지 입력 가능합니다.")
    @Schema(description = "투표 옵션 내용", example = "찬성", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;
}
