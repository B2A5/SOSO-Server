package com.example.soso.users.domain.dto;

import com.example.soso.users.domain.entity.BudgetRange;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "예산 설정 요청")
public record BudgetRequest(

        @Schema(description = "예산 구간 (선택 사항)", example = "THOUSANDS_3000_5000")
        BudgetRange budget

) {}
