package com.example.soso.community.freeboard.post.domain.dto;

import com.example.soso.community.common.post.domain.entity.Category;
import com.example.soso.users.domain.entity.UserType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "자유게시판 글 상세 조회 응답")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FreeboardDetailResponse {

    @Schema(description = "게시글 ID", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long postId;

    @Schema(description = "작성자 정보", requiredMode = Schema.RequiredMode.REQUIRED)
    private PostDetailAuthorInfo author;

    @Schema(description = "카테고리", example = "restaurant", requiredMode = Schema.RequiredMode.REQUIRED)
    private Category category;

    @Schema(description = "제목", example = "맛있는 라면집 추천해요!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "내용", example = "어제 갔던 라면집이 정말 맛있어서...", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @Schema(description = "첨부된 이미지 정보 목록", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<ImageInfo> images;

    @Schema(description = "좋아요 수", example = "15", requiredMode = Schema.RequiredMode.REQUIRED)
    private int likeCount;

    @Schema(description = "댓글 수", example = "8", requiredMode = Schema.RequiredMode.REQUIRED)
    private int commentCount;

    @Schema(description = "조회 수", example = "102", requiredMode = Schema.RequiredMode.REQUIRED)
    private int viewCount;

    @Schema(description = "작성 시간", example = "2024-12-25T10:30:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createdAt;

    @Schema(description = "수정 시간", example = "2024-12-25T14:20:00", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private LocalDateTime updatedAt;

    @Schema(description = "요청한 사용자가 인증되었는지 여부 (액세스 토큰 제공 여부)", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean isAuthorized;

    @Schema(description = "작성자 여부 (현재 사용자가 작성자인지)", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean isAuthor;

    @Schema(
        description = "현재 사용자의 좋아요 여부 (비인증 사용자인 경우 null)",
        example = "true",
        requiredMode = Schema.RequiredMode.REQUIRED,
        nullable = true
    )
    @JsonProperty("isLiked")
    private Boolean isLiked;

    @Schema(
        description = "편집 가능 여부 (비인증 사용자인 경우 null)",
        example = "true",
        requiredMode = Schema.RequiredMode.REQUIRED,
        nullable = true
    )
    private Boolean canEdit;

    @Schema(
        description = "삭제 가능 여부 (비인증 사용자인 경우 null)",
        example = "true",
        requiredMode = Schema.RequiredMode.REQUIRED,
        nullable = true
    )
    private Boolean canDelete;

    // Lombok @Getter가 생성하는 isLiked() 메서드를 Jackson이 "liked"로 직렬화하는 것을 방지
    @JsonIgnore
    public Boolean isLiked() {
        return isLiked;
    }

    @JsonIgnore
    public Boolean isCanEdit() {
        return canEdit;
    }

    @JsonIgnore
    public Boolean isCanDelete() {
        return canDelete;
    }

    @Schema(description = "이미지 정보")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ImageInfo {
        @Schema(description = "이미지 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long imageId;

        @Schema(description = "이미지 URL", example = "https://s3.amazonaws.com/bucket/image.jpg", requiredMode = Schema.RequiredMode.REQUIRED)
        private String imageUrl;

        @Schema(description = "이미지 순서", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        private int sequence;
    }

    @Schema(description = "작성자 정보")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PostDetailAuthorInfo {
        @Schema(description = "작성자 ID", example = "user123", requiredMode = Schema.RequiredMode.REQUIRED)
        private String userId;

        @Schema(description = "작성자 닉네임", example = "맛집탐험가", requiredMode = Schema.RequiredMode.REQUIRED)
        private String nickname;

        @Schema(description = "작성자 프로필 이미지 URL", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        private String profileImageUrl;

        @Schema(description = "작성자 유형", example = "INHABITANT", requiredMode = Schema.RequiredMode.REQUIRED)
        private UserType userType;

        @Schema(description = "작성자 주소", example = "서울시 강남구", requiredMode = Schema.RequiredMode.REQUIRED)
        private String address;
    }
}