package com.example.soso.users.domain.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "연령대 구간")
public enum AgeRange {

    @Schema(description = "10대")
    TEENS,

    @Schema(description = "20대")
    TWENTIES,

    @Schema(description = "30대")
    THIRTIES,

    @Schema(description = "40대")
    FORTIES,

    @Schema(description = "50대")
    FIFTIES,

    @Schema(description = "60대 이상")
    SIXTIES_AND_OVER
}
