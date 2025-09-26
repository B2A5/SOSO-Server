package com.example.soso.community.freeboard.comment.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "자유게시판 댓글 커서 기반 목록 조회 응답")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FreeboardCommentCursorResponse {

    @Schema(description = "댓글 목록")
    private List<FreeboardCommentSummary> comments;

    @Schema(description = "다음 페이지 존재 여부", example = "true")
    private boolean hasNext;

    @Schema(description = "다음 페이지를 위한 커서 값")
    private String nextCursor;

    @Schema(description = "현재 페이지 크기", example = "20")
    private int size;

    @Schema(description = "댓글 요약 정보")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FreeboardCommentSummary {
        @Schema(description = "댓글 ID", example = "456")
        private Long commentId;

        @Schema(description = "게시글 ID", example = "123")
        private Long postId;

        @Schema(description = "부모 댓글 ID (대댓글인 경우)", example = "789")
        private Long parentCommentId;

        @Schema(description = "작성자 정보")
        private CommentAuthorInfo author;

        @Schema(description = "댓글 내용", example = "좋은 정보 감사합니다!")
        private String content;

        @Schema(description = "대댓글 수", example = "3")
        private int replyCount;

        @Schema(description = "댓글 깊이 (0: 일반 댓글, 1: 대댓글)", example = "0")
        private int depth;

        @Schema(description = "삭제된 댓글 여부", example = "false")
        private boolean deleted;

        @Schema(description = "현재 사용자가 작성한 댓글인지", example = "true")
        private boolean isAuthor;

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
    public static class CommentAuthorInfo {
        @Schema(description = "작성자 ID", example = "user123")
        private String userId;

        @Schema(description = "작성자 닉네임", example = "댓글러")
        private String nickname;

        @Schema(description = "작성자 프로필 이미지 URL")
        private String profileImageUrl;
    }
}