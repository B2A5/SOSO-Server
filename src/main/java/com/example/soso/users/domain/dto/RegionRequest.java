package com.example.soso.users.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RegionRequest {

    @NotBlank(message = "지역은 필수입니다.")
    private String regionId;
}

