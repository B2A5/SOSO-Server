package com.example.soso.users.domain.entity;

import com.fasterxml.jackson.annotation.JsonValue;

public enum BudgetRange {
    UNDER_1000("1천 이하"),
    THOUSANDS_2000("2천대"),
    THOUSANDS_3000_5000("3~5천"),
    THOUSANDS_5000_7000("5천~7천"),
    THOUSANDS_7000_TO_1B("7천~1억"),
    OVER_1B("1억 이상");

    private final String label;

    BudgetRange(String label) {
        this.label = label;
    }
}
