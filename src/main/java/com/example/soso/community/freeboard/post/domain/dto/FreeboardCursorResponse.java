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

    @Schema(description = "게시글 목록")
    private List<FreeboardSummary> posts;

    @Schema(description = "다음 페이지 존재 여부", example = "true")
    private boolean hasNext;

    @Schema(description = "다음 페이지를 위한 커서 값")
    private String nextCursor;

    @Schema(description = "현재 페이지 크기", example = "10")
    private int size;

    @Schema(description = "총 게시글 수", example = "150")
    private long totalCount;

    @Schema(description = "게시글 요약 정보")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FreeboardSummary {
        @Schema(description = "게시글 ID", example = "123")
        private Long postId;

        @Schema(description = "작성자 정보")
        private PostAuthorInfo author;

        @Schema(description = "카테고리", example = "restaurant")
        private Category category;

        @Schema(description = "제목", example = "맛있는 라면집 추천해요!")
        private String title;

        @Schema(description = "내용 미리보기 (100자 제한)", example = "어제 갔던 라면집이 정말 맛있어서 공유합니다. 국물이 진하고...")
        private String contentPreview;

        @Schema(description = "첫 번째 이미지 URL (썸네일용)")
        private String thumbnailUrl;

        @Schema(description = "이미지 개수", example = "3")
        private int imageCount;

        @Schema(description = "좋아요 수", example = "15")
        private int likeCount;

        @Schema(description = "댓글 수", example = "8")
        private int commentCount;

        @Schema(description = "조회 수", example = "102")
        private int viewCount;

        @Schema(description = "현재 사용자의 좋아요 여부", example = "false")
        private boolean isLiked;

        @Schema(description = "작성 시간", example = "2024-12-25T10:30:00")
        private LocalDateTime createdAt;

        @Schema(description = "수정 시간", example = "2024-12-25T14:20:00")
        private LocalDateTime updatedAt;
    }

    @Schema(description = "작성자 정보")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PostAuthorInfo {
        @Schema(description = "작성자 ID", example = "user123")
        private String userId;

        @Schema(description = "작성자 닉네임", example = "맛집탐험가")
        private String nickname;

        @Schema(description = "작성자 프로필 이미지 URL")
        private String profileImageUrl;

        @Schema(description = "작성자 유형", example = "INHABITANT")
        private UserType userType;
    }
}