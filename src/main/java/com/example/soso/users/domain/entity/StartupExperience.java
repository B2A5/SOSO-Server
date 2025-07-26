package com.example.soso.users.domain.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "창업 경험 여부")
public enum StartupExperience {

    @Schema(description = "창업 경험 있음")
    YES("창업 경험 유"),

    @Schema(description = "창업 경험 없음")
    NO("창업 경험 무");

    private final String label;

    StartupExperience(String label) {
        this.label = label;
    }
}
