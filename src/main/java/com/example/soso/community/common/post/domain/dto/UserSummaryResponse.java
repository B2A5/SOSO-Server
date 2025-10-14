package com.example.soso.community.common.post.domain.dto;

import com.example.soso.users.domain.entity.UserType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "작성자 요약 정보 DTO")
public record UserSummaryResponse(

        @Schema(description = "사용자 ID", example = "user123", requiredMode = Schema.RequiredMode.REQUIRED)
        String userId,

        @Schema(description = "닉네임", example = "소소한개발자", requiredMode = Schema.RequiredMode.REQUIRED)
        String nickname,

        @Schema(description = "지역", example = "서울특별시 강남구", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String location,

        @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String profileImageUrl,

        @Schema(description = "창업자인지, 주민이지", requiredMode = Schema.RequiredMode.REQUIRED)
        UserType userType

) {


}
