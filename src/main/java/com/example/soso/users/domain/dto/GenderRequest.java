package com.example.soso.users.domain.dto;

import com.example.soso.users.domain.entity.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "성별 설정 요청")
public record GenderRequest(

        @NotNull(message = "성별은 필수입니다.")
        @Schema(description = "성별", example = "FEMALE")
        Gender gender

) {}
