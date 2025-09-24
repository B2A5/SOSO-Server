package com.example.soso.users.domain.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "관심 업종 설정 요청")
public record InterestRequest(

        @Schema(description = "관심 업종 목록 (빈 배열 허용)",
                example = "[\"ACCOMMODATION_FOOD\", \"MANUFACTURING\"]")
        List<InterestType> interests

) {}
