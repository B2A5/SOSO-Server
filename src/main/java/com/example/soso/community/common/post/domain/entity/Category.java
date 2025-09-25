package com.example.soso.community.common.post.domain.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(
    description = "자유게시판 카테고리",
    allowableValues = {
        "daily-hobby", "restaurant", "living-convenience",
        "neighborhood-news", "startup", "others"
    },
    example = "restaurant"
)
@Getter
public enum Category {

    @Schema(description = "일상/취미", example = "daily-hobby")
    DAILY_HOBBY("daily-hobby", "일상/취미"),

    @Schema(description = "맛집", example = "restaurant")
    RESTAURANT("restaurant", "맛집"),

    @Schema(description = "생활/꿀팁", example = "living-convenience")
    LIVING_CONVENIENCE("living-convenience", "생활/꿀팁"),

    @Schema(description = "동네소식", example = "neighborhood-news")
    NEIGHBORHOOD_NEWS("neighborhood-news", "동네소식"),

    @Schema(description = "창업", example = "startup")
    STARTUP("startup", "창업"),

    @Schema(description = "기타", example = "others")
    OTHERS("others", "기타");

    private final String value;
    private final String label;

    Category(String value, String label) {
        this.value = value;
        this.label = label;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static Category fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        // enum 이름으로 먼저 시도
        for (Category category : Category.values()) {
            if (category.name().equals(value)) {
                return category;
            }
        }

        // value로 시도
        for (Category category : Category.values()) {
            if (category.getValue().equals(value)) {
                return category;
            }
        }

        // label로 시도
        for (Category category : Category.values()) {
            if (category.getLabel().equals(value)) {
                return category;
            }
        }

        throw new IllegalArgumentException("Unknown Category: " + value);
    }
}
