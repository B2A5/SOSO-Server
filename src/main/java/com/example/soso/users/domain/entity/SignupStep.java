package com.example.soso.users.domain.entity;

public enum SignupStep {
    USER_TYPE,       // 예비창업자 / 주민 선택
    REGION,          // 지역 선택
    AGE,
    GENDER,  // 연령대 + 성별 선택
    INTERESTS,       // 관심 업종 선택
    BUDGET,           // 예산 입력 (또는 건너뛰기)
    STARTUP,
    NINAME,
    COMPLETE
}
