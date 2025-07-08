package com.example.soso.users.domain.dto;

import com.example.soso.users.domain.entity.AgeRange;
import jakarta.validation.constraints.NotNull;

public record AgeRangeRequest(
        @NotNull(message = "연령대는 필수입니다.")
        AgeRange ageRange
) {}

