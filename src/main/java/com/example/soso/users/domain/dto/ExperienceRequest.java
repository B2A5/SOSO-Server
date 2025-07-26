package com.example.soso.users.domain.dto;

import com.example.soso.users.domain.entity.StartupExperience;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "창업 경험 설정 요청")
public record ExperienceRequest(

        @NotNull(message = "창업경험은 필수 입니다.")
        @Schema(description = "창업 경험 여부", example = "YES")
        StartupExperience experience

) {}
