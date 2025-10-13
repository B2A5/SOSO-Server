package com.example.soso.users.domain.dto;

import com.example.soso.users.domain.entity.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@Schema(description = "사용자 정보 응답")
public class UserResponse {

    @Schema(description = "사용자 ID (UUID)", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @Schema(description = "사용자명", example = "홍길동", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @Schema(description = "닉네임", example = "길동이", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nickname;

    @Schema(description = "이메일", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @Schema(description = "사용자 유형 (FOUNDER: 예비 창업자, INHABITANT: 일반 거주민)", example = "FOUNDER", requiredMode = Schema.RequiredMode.REQUIRED)
    private UserType userType;

    @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
    private String profileImageUrl;

    @Schema(description = "성별 (MALE: 남성, FEMALE: 여성, NONE: 선택 안함)", example = "MALE", requiredMode = Schema.RequiredMode.REQUIRED)
    private Gender gender;

    @Schema(description = "연령대", example = "TWENTIES", requiredMode = Schema.RequiredMode.REQUIRED)
    private AgeRange ageRange;

    @Schema(description = "예산 구간", example = "3~5천")
    private String budget;

    @Schema(description = "창업 경험 여부", example = "창업 경험 유")
    private String startupExperience;

    @Schema(description = "지역명", example = "서울시 강남구", requiredMode = Schema.RequiredMode.REQUIRED)
    private String location;

    @Schema(description = "관심 업종 목록")
    private List<String> interests;

    @Schema(description = "위도", example = "37.5665")
    private String latitude;

    @Schema(description = "경도", example = "126.9780")
    private String longitude;

    @Schema(description = "계정 생성일시", example = "2024-01-01T00:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createdDate;

    @Schema(description = "마지막 수정일시", example = "2024-01-01T00:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime lastModifiedDate;
}
