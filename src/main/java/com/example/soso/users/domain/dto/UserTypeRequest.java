package com.example.soso.users.domain.dto;

import com.example.soso.users.domain.entity.UserType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "유저 타입 설정 요청")
public record UserTypeRequest(

        @NotNull(message = "창업자 또는 거주민 선택해주세요")
        @Schema(description = "유저 타입", example = "FOUNDER")
        UserType userType

) {}
