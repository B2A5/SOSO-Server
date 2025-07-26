package com.example.soso.users.domain.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "사용자 유형")
public enum UserType {
    @Schema(description = "예비 창업자")
    FOUNDER,

    @Schema(description = "일반 거주민")
    INHABITANT
}
