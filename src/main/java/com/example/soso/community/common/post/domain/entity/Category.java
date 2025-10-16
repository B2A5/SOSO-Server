package com.example.soso.community.common.post.domain.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description = "자유게시판 카테고리",
    allowableValues = {
        "daily-hobby", "restaurant", "living-convenience",
        "neighborhood-news", "startup", "others"
    },
    example = "restaurant"
)
public enum Category {

    @Schema(description = "일상/취미")
    DAILY_HOBBY("daily-hobby"),

    @Schema(description = "맛집")
    RESTAURANT("restaurant"),

    @Schema(description = "생활/꿀팁")
    LIVING_CONVENIENCE("living-convenience"),

    @Schema(description = "동네소식")
    NEIGHBORHOOD_NEWS("neighborhood-news"),

    @Schema(description = "창업")
    STARTUP("startup"),

    @Schema(description = "기타")
    OTHERS("others");

    private final String value;

    Category(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static Category fromValue(String value) {
        if (value == null) {
            return null;
        }

        // 케밥 케이스 소문자로 매칭
        for (Category category : Category.values()) {
            if (category.value.equalsIgnoreCase(value)) {
                return category;
            }
        }

        // 대문자 스네이크 케이스로도 매칭 (하위 호환성)
        for (Category category : Category.values()) {
            if (category.name().equalsIgnoreCase(value)) {
                return category;
            }
        }

        throw new IllegalArgumentException("Unknown category value: " + value +
            ". Allowed values are: daily-hobby, restaurant, living-convenience, neighborhood-news, startup, others");
    }
}
