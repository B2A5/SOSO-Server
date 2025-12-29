package com.example.soso.community.voteboard.domain.dto;

import com.example.soso.community.common.post.domain.entity.Category;
import com.example.soso.community.common.post.domain.dto.UserSummaryResponse;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 투표 게시판 게시글 요약 정보
 */
@Schema(description = "투표 게시판 게시글 요약 정보")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoteboardSummary {

    @Schema(description = "게시글 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long postId;

    @Schema(description = "작성자 정보", requiredMode = Schema.RequiredMode.REQUIRED)
    private UserSummaryResponse author;

    @Schema(description = "카테고리", example = "restaurant", requiredMode = Schema.RequiredMode.REQUIRED)
    private Category category;

    @Schema(description = "게시글 제목", example = "우리 동네 카페 선호도 조사", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "내용 미리보기 (100자 제한)", example = "여러분이 가장 좋아하는 카페 스타일은 무엇인가요? 조용한 분위기, 활기찬 분위기...", requiredMode = Schema.RequiredMode.REQUIRED)
    private String contentPreview;

    @Schema(description = "첫 번째 이미지 URL (썸네일용)", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String thumbnailUrl;

    @Schema(description = "이미지 개수", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    private int imageCount;

    @Schema(description = "조회수", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
    private int viewCount;

    @Schema(description = "댓글 수", example = "45", requiredMode = Schema.RequiredMode.REQUIRED)
    private long commentCount;

    @Schema(
        description = "현재 사용자의 투표 참여 여부 (비인증 사용자인 경우 null, 참여하지 않은 경우 false, 참여한 경우 true)",
        example = "true",
        requiredMode = Schema.RequiredMode.REQUIRED,
        nullable = true
    )
    private Boolean hasVoted;

    @Schema(description = "투표 정보", requiredMode = Schema.RequiredMode.REQUIRED)
    private VoteInfo voteInfo;

    @Schema(description = "투표 옵션 목록 (미리보기, 최대 3개)", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<VoteOptionResponse> voteOptions;

    @Schema(description = "좋아요 수", example = "23", requiredMode = Schema.RequiredMode.REQUIRED)
    private long likeCount;

    @Schema(
        description = "현재 사용자의 좋아요 여부 (비인증 사용자인 경우 null)",
        example = "false",
        requiredMode = Schema.RequiredMode.REQUIRED,
        nullable = true
    )
    @JsonProperty("isLiked")
    private Boolean isLiked;

    @Schema(description = "생성일시", example = "2024-01-01T10:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2024-01-02T15:30:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime updatedAt;

    // Lombok @Getter가 생성하는 isLiked() 메서드를 Jackson이 "liked"로 직렬화하는 것을 방지
    @JsonIgnore
    public Boolean isLiked() {
        return isLiked;
    }
}
