package com.example.soso.community.votesboard.domain.dto;

import com.example.soso.community.common.post.domain.entity.Category;
import com.example.soso.community.common.post.domain.dto.UserSummaryResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 투표 게시판 게시글 상세 정보
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "투표 게시판 게시글 상세 정보")
public class VoteboardDetailResponse {

    @Schema(description = "게시글 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long postId;

    @Schema(description = "작성자 정보", requiredMode = Schema.RequiredMode.REQUIRED)
    private UserSummaryResponse author;

    @Schema(description = "카테고리", example = "restaurant", requiredMode = Schema.RequiredMode.REQUIRED)
    private Category category;

    @Schema(description = "게시글 제목", example = "우리 동네 카페 선호도 조사", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "게시글 내용", example = "여러분이 가장 좋아하는 카페 스타일은 무엇인가요?", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @Schema(description = "첨부된 이미지 정보 목록", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<ImageInfo> images;

    @Schema(description = "투표 옵션 목록", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<VoteOptionResponse> voteOptions;

    @Schema(
        description = "현재 사용자의 투표 참여 여부 (비인증 사용자인 경우 null, 참여하지 않은 경우 false, 참여한 경우 true)",
        example = "true",
        requiredMode = Schema.RequiredMode.REQUIRED,
        nullable = true
    )
    private Boolean hasVoted;

    @Schema(description = "투표 정보", requiredMode = Schema.RequiredMode.REQUIRED)
    private VoteInfo voteInfo;

    @Schema(description = "조회수", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
    private int viewCount;

    @Schema(description = "댓글 수", example = "45", requiredMode = Schema.RequiredMode.REQUIRED)
    private long commentCount;

    @Schema(description = "좋아요 수", example = "42", requiredMode = Schema.RequiredMode.REQUIRED)
    private long likeCount;

    @Schema(description = "현재 사용자의 좋아요 여부 (비인증 사용자인 경우 null)", example = "true", requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
    @JsonProperty("isLiked")
    private Boolean isLiked;

    @Schema(description = "요청한 사용자가 인증되었는지 여부 (액세스 토큰 제공 여부)", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean isAuthorized;

    @Schema(description = "현재 사용자가 게시글 작성자인지 여부 (비인증 사용자인 경우 false)", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean isAuthor;

    @Schema(description = "현재 사용자가 게시글 수정 권한이 있는지 여부 (비인증 사용자인 경우 null, 작성자인 경우 true)", example = "false", requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
    private Boolean canEdit;

    @Schema(description = "현재 사용자가 게시글 삭제 권한이 있는지 여부 (비인증 사용자인 경우 null, 작성자인 경우 true)", example = "false", requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
    private Boolean canDelete;

    @Schema(description = "생성일시", example = "2024-01-01T10:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2024-01-02T15:30:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime updatedAt;

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
}
