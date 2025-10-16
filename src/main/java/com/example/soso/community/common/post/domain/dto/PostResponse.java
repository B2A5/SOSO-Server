package com.example.soso.community.common.post.domain.dto;

import com.example.soso.community.common.post.domain.entity.Category;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "게시글 상세 조회 응답 DTO")
public record PostResponse(

        @Schema(description = "게시글 ID", example = "101", requiredMode = Schema.RequiredMode.REQUIRED)
        Long postId,

        @Schema(description = "제목", example = "동네 소식 공유해요", requiredMode = Schema.RequiredMode.REQUIRED)
        String title,

        @Schema(description = "내용", example = "오늘 우리 아파트에서 작은 장터가 열립니다.", requiredMode = Schema.RequiredMode.REQUIRED)
        String content,

        @Schema(description = "카테고리", example = "restaurant", requiredMode = Schema.RequiredMode.REQUIRED)
        Category category,

        @Schema(description = "이미지 URL 목록", example = "[\"https://example.com/image1.jpg\"]", requiredMode = Schema.RequiredMode.REQUIRED)
        List<String> imageUrls,

        @Schema(description = "게시글 좋아요 수", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
        int likeCount,

        @Schema(description = "내가 게시글 좋아요를 눌렀는지 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean isLiked,

        @Schema(description = "작성일시 (ISO 형식)", example = "2025-07-26T10:30:00", requiredMode = Schema.RequiredMode.REQUIRED)
        String createdAt,

        @Schema(description = "작성자 요약 정보", requiredMode = Schema.RequiredMode.REQUIRED)
        UserSummaryResponse user

) {}
