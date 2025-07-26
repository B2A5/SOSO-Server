package com.example.soso.users.domain.entity;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관심 업종 유형")
public enum InterestType {

    @Schema(description = "식료품, 생활용품 등 제조업")
    MANUFACTURING("식료품 등 제조업"),

    @Schema(description = "도소매 유통업")
    WHOLESALE_RETAIL("도매 및 소매업"),

    @Schema(description = "운수/물류업")
    TRANSPORT("운수업"),

    @Schema(description = "숙박업 및 음식점업")
    ACCOMMODATION_FOOD("숙박업 및 음식점업"),

    @Schema(description = "보건 및 사회복지 서비스업")
    WELFARE("보건 및 사회 복지업"),

    @Schema(description = "예술, 스포츠 관련 서비스업")
    ART_SPORTS("예술 및 스포츠업"),

    @Schema(description = "기타")
    OTHER("기타");

    private final String label;

    InterestType(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }
}
