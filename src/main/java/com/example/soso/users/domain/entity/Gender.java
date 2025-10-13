package com.example.soso.users.domain.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "성별")
public enum Gender {

    @Schema(description = "남성")
    MALE,

    @Schema(description = "여성")
    FEMALE,

    @Schema(description = "선택 안함")
    NONE
}
