package com.example.soso.users.domain.dto;

import com.example.soso.users.domain.entity.AgeRange;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "연령대 설정 요청")
public record AgeRangeRequest(

        @NotNull(message = "연령대는 필수입니다.")
        @Schema(description = "연령대", example = "TWENTIES")
        AgeRange ageRange

) {}
