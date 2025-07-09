package com.example.soso.users.domain.dto;

import com.example.soso.users.domain.entity.StartupExperience;
import jakarta.validation.constraints.NotNull;

public record ExperienceRequest(

        @NotNull(message = "창업경험은 필수 입니다.")
        StartupExperience experience
) {
}
