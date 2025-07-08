package com.example.soso.users.domain.dto;

import com.example.soso.users.domain.entity.UserType;
import jakarta.validation.constraints.NotNull;

public record UserTypeRequest(

        @NotNull(message = "창업자 또는 거주민 선택해주세요")
        UserType userType
) {
}
