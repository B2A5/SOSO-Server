package com.example.soso.users.domain.entity;

/**
 * 회원가입 단계 식별용 Enum.
 * 순서는 SignupFlow의 List 정의와 반드시 일치해야 하며,
 * 서비스 검증 로직이 ordinal 대신 Enum 값 자체를 사용하도록 구성되어 있다.
 */
public enum SignupStep {
    USER_TYPE,       // 예비창업자 / 주민 선택
    REGION,          // 지역 선택
    AGE,             // 연령대 선택
    GENDER,          // 성별 선택
    INTERESTS,       // 관심 업종 선택 (Founder 전용)
    BUDGET,          // 예산 입력 (Founder 전용, 선택 사항)
    STARTUP,         // 창업 경험 여부 (Founder 전용)
    NICKNAME,        // 닉네임 생성
    COMPLETE         // 회원가입 완료
}
