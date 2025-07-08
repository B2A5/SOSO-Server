package com.example.soso.users.domain.dto;


import com.example.soso.users.domain.entity.Gender;
import jakarta.validation.constraints.NotNull;

public record GenderRequest(
        @NotNull(message = "성별은 필수입니다.")
        Gender gender
) {}

