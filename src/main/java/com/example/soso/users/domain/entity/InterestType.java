package com.example.soso.users.domain.entity;

import com.fasterxml.jackson.annotation.JsonValue;

public enum InterestType {
    MANUFACTURING("식료품 등 제조업"),
    WHOLESALE_RETAIL("도매 및 소매업"),
    TRANSPORT("운수업"),
    ACCOMMODATION_FOOD("숙박업 및 음식점업"),
    WELFARE("보건 및 사회 복지업"),
    ART_SPORTS("예술 및 스포츠업"),
    OTHER("기타");

    private final String label;

    InterestType(String label) {
        this.label = label;
    }
}
