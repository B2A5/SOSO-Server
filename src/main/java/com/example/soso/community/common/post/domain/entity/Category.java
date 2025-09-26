package com.example.soso.community.common.post.domain.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description = "자유게시판 카테고리",
    allowableValues = {
        "DAILY_HOBBY", "RESTAURANT", "LIVING_CONVENIENCE",
        "NEIGHBORHOOD_NEWS", "STARTUP", "OTHERS"
    },
    example = "RESTAURANT"
)
public enum Category {

    @Schema(description = "일상/취미")
    DAILY_HOBBY,

    @Schema(description = "맛집")
    RESTAURANT,

    @Schema(description = "생활/꿀팁")
    LIVING_CONVENIENCE,

    @Schema(description = "동네소식")
    NEIGHBORHOOD_NEWS,

    @Schema(description = "창업")
    STARTUP,

    @Schema(description = "기타")
    OTHERS
}
