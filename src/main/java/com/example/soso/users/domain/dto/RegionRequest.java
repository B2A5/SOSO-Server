package com.example.soso.users.domain.dto;

import jakarta.validation.constraints.NotBlank;

public record RegionRequest(

        @NotBlank(message = "regionId는 필수입니다.")
        String regionId
) {}

