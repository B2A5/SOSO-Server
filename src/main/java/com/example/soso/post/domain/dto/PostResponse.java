package com.example.soso.post.domain.dto;

import com.example.soso.post.domain.dto.UserSummaryResponse;
import com.example.soso.post.domain.entity.Category;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "게시글 조회 응답 DTO")
public record PostResponse(

        @Schema(description = "게시글 ID", example = "101")
        Long postId,

        @Schema(description = "제목", example = "동네 소식 공유해요")
        String title,

        @Schema(description = "내용", example = "오늘 우리 아파트에서 작은 장터가 열립니다.")
        String content,

        @Schema(description = "카테고리", example = "MARKET")
        Category category,

        @Schema(description = "이미지 URL 목록", example = "[\"https://example.com/image1.jpg\"]")
        List<String> imageUrls,

        @Schema(description = "좋아요 수", example = "10")
        int likeCount,

        @Schema(description = "댓글 수", example = "3")
        int commentCount,

        @Schema(description = "작성일시 (ISO 형식)", example = "2025-07-26T10:30:00")
        String createdAt,

        @Schema(description = "작성자 요약 정보")
        UserSummaryResponse user

) {}
