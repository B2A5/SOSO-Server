package com.example.soso.community.pollboard.comment.domain.dto;

import com.example.soso.users.domain.entity.UserType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "투표 게시판 댓글 커서 기반 목록 조회 응답")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollCommentCursorResponse {

    @Schema(description = "댓글 목록", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<PollCommentSummary> comments;

    @Schema(description = "다음 페이지 존재 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean hasNext;

    @Schema(description = "다음 페이지를 위한 커서 값", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String nextCursor;

    @Schema(description = "현재 페이지 크기", example = "20", requiredMode = Schema.RequiredMode.REQUIRED)
    private int size;

    @Schema(description = "총 댓글 수 (삭제된 댓글 포함)", example = "50", requiredMode = Schema.RequiredMode.REQUIRED)
    private long total;

    @Schema(description = "요청한 사용자가 인증되었는지 여부 (액세스 토큰 제공 여부)", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("isAuthorized")
    private boolean isAuthorized;

    @Schema(description = "댓글 요약 정보")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PollCommentSummary {
        @Schema(description = "댓글 ID", example = "456", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long commentId;

        @Schema(description = "투표 게시글 ID", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long pollId;

        @Schema(description = "부모 댓글 ID (대댓글인 경우)", example = "789", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        private Long parentCommentId;

        @Schema(description = "작성자 정보", requiredMode = Schema.RequiredMode.REQUIRED)
        private CommentAuthorInfo author;

        @Schema(description = "댓글 내용", example = "좋은 투표 주제네요!", requiredMode = Schema.RequiredMode.REQUIRED)
        private String content;

        @Schema(description = "대댓글 수", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
        private int replyCount;

        @Schema(description = "댓글 좋아요 수", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
        private int likeCount;

        @Schema(description = "댓글 깊이 (0: 일반 댓글, 1: 대댓글)", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        private int depth;

        @Schema(description = "삭제된 댓글 여부", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
        private boolean deleted;

        @Schema(description = "현재 사용자가 작성한 댓글인지", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("isAuthor")
        private boolean isAuthor;

        @Schema(
            description = "현재 사용자의 댓글 좋아요 여부 (비인증 사용자인 경우 null)",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED,
            nullable = true
        )
        @JsonProperty("isLiked")
        @JsonInclude(JsonInclude.Include.ALWAYS)
        private Boolean isLiked;

        @Schema(
            description = "댓글 수정 가능 여부 (비인증 사용자인 경우 null)",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED,
            nullable = true
        )
        @JsonInclude(JsonInclude.Include.ALWAYS)
        private Boolean canEdit;

        @Schema(
            description = "댓글 삭제 가능 여부 (비인증 사용자인 경우 null)",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED,
            nullable = true
        )
        @JsonInclude(JsonInclude.Include.ALWAYS)
        private Boolean canDelete;

        @Schema(description = "작성 시간", example = "2024-12-25T10:30:00", requiredMode = Schema.RequiredMode.REQUIRED)
        private LocalDateTime createdAt;

        @Schema(description = "수정 시간", example = "2024-12-25T14:20:00", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        private LocalDateTime updatedAt;

        public Boolean isLiked() {
            return isLiked;
        }

        public Boolean isCanEdit() {
            return canEdit;
        }

        public Boolean isCanDelete() {
            return canDelete;
        }
    }

    @Schema(description = "작성자 정보")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CommentAuthorInfo {
        @Schema(description = "작성자 ID", example = "user123", requiredMode = Schema.RequiredMode.REQUIRED)
        private String userId;

        @Schema(description = "작성자 닉네임", example = "댓글러", requiredMode = Schema.RequiredMode.REQUIRED)
        private String nickname;

        @Schema(description = "작성자 프로필 이미지 URL", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        private String profileImageUrl;

        @Schema(description = "작성자 유형", example = "INHABITANT", requiredMode = Schema.RequiredMode.REQUIRED)
        private UserType userType;
    }
}
