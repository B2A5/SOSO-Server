package com.example.soso.users.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "지역 설정 요청")
public record RegionRequest(

        @NotBlank(message = "regionId는 필수입니다.")
        @Schema(description = "지역 ID", example = "11010")
        String regionId

) {}
