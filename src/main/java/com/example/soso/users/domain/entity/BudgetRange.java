package com.example.soso.users.domain.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "예산 구간")
public enum BudgetRange {

    @Schema(description = "1천 이하")
    UNDER_1000("1천 이하"),

    @Schema(description = "2천대")
    THOUSANDS_2000("2천대"),

    @Schema(description = "3천 ~ 5천")
    THOUSANDS_3000_5000("3~5천"),

    @Schema(description = "5천 ~ 7천")
    THOUSANDS_5000_7000("5천~7천"),

    @Schema(description = "7천 ~ 1억")
    THOUSANDS_7000_TO_1B("7천~1억"),

    @Schema(description = "1억 이상")
    OVER_1B("1억 이상");

    private final String label;

    BudgetRange(String label) {
        this.label = label;
    }
}
