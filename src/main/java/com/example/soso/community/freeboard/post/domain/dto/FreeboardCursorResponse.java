package com.example.soso.community.freeboard.post.domain.dto;

import com.example.soso.community.common.post.domain.entity.Category;
import com.example.soso.users.domain.entity.UserType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "자유게시판 커서 기반 목록 조회 응답")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FreeboardCursorResponse {

    @Schema(description = "게시글 목록", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<FreeboardSummary> posts;

    @Schema(description = "다음 페이지 존재 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean hasNext;

    @Schema(description = "다음 페이지를 위한 커서 값", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String nextCursor;

    @Schema(description = "현재 페이지 크기", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    private int size;

    @Schema(description = "총 게시글 수", example = "150", requiredMode = Schema.RequiredMode.REQUIRED)
    private long totalCount;

    @Schema(description = "게시글 요약 정보")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FreeboardSummary {
        @Schema(description = "게시글 ID", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long postId;

        @Schema(description = "작성자 정보", requiredMode = Schema.RequiredMode.REQUIRED)
        private PostAuthorInfo author;

        @Schema(description = "카테고리", example = "restaurant", requiredMode = Schema.RequiredMode.REQUIRED)
        private Category category;

        @Schema(description = "제목", example = "맛있는 라면집 추천해요!", requiredMode = Schema.RequiredMode.REQUIRED)
        private String title;

        @Schema(description = "내용 미리보기 (100자 제한)", example = "어제 갔던 라면집이 정말 맛있어서 공유합니다. 국물이 진하고...", requiredMode = Schema.RequiredMode.REQUIRED)
        private String contentPreview;

        @Schema(description = "첫 번째 이미지 URL (썸네일용)", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        private String thumbnailUrl;

        @Schema(description = "이미지 개수", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
        private int imageCount;

        @Schema(description = "좋아요 수", example = "15", requiredMode = Schema.RequiredMode.REQUIRED)
        private int likeCount;

        @Schema(description = "댓글 수", example = "8", requiredMode = Schema.RequiredMode.REQUIRED)
        private int commentCount;

        @Schema(description = "조회 수", example = "102", requiredMode = Schema.RequiredMode.REQUIRED)
        private int viewCount;

        @Schema(description = "현재 사용자의 좋아요 여부", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
        private boolean isLiked;

        @Schema(description = "작성 시간", example = "2024-12-25T10:30:00", requiredMode = Schema.RequiredMode.REQUIRED)
        private LocalDateTime createdAt;

        @Schema(description = "수정 시간", example = "2024-12-25T14:20:00", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        private LocalDateTime updatedAt;
    }

    @Schema(description = "작성자 정보")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PostAuthorInfo {
        @Schema(description = "작성자 ID", example = "user123", requiredMode = Schema.RequiredMode.REQUIRED)
        private String userId;

        @Schema(description = "작성자 닉네임", example = "맛집탐험가", requiredMode = Schema.RequiredMode.REQUIRED)
        private String nickname;

        @Schema(description = "작성자 프로필 이미지 URL", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        private String profileImageUrl;

        @Schema(description = "작성자 유형", example = "INHABITANT", requiredMode = Schema.RequiredMode.REQUIRED)
        private UserType userType;
    }
}