package com.example.soso.users.domain.dto;

import com.example.soso.users.domain.entity.BudgetRange;

public record BudgetRequest(
        BudgetRange budget // null 허용
) {}

