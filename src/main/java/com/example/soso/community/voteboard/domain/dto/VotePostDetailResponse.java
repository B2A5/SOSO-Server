package com.example.soso.community.voteboard.domain.dto;

import com.example.soso.community.voteboard.domain.entity.VoteStatus;
import com.example.soso.community.common.post.domain.dto.UserSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 투표 게시글 상세 응답 DTO
 */
@Getter
@Builder
@Schema(description = "투표 게시글 상세 정보")
public class VotePostDetailResponse {

    @Schema(description = "게시글 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "게시글 제목", example = "우리 동네 카페 선호도 조사", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "게시글 내용", example = "여러분이 가장 좋아하는 카페 스타일은 무엇인가요?", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @Schema(description = "작성자 정보", requiredMode = Schema.RequiredMode.REQUIRED)
    private UserSummaryResponse author;

    @Schema(description = "이미지 URL 목록", example = "[\"https://example.com/image1.jpg\", \"https://example.com/image2.jpg\"]")
    private List<String> imageUrls;

    @Schema(description = "투표 옵션 목록", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<VoteOptionResponse> voteOptions;

    @Schema(description = "현재 사용자가 선택한 옵션 ID (미투표 시 null)", example = "1")
    private Long selectedOptionId;

    @Schema(description = "총 투표 참여자 수", example = "89", requiredMode = Schema.RequiredMode.REQUIRED)
    private int totalVotes;

    @Schema(description = "투표 상태 (IN_PROGRESS: 진행중, COMPLETED: 완료)", example = "IN_PROGRESS", requiredMode = Schema.RequiredMode.REQUIRED)
    private VoteStatus voteStatus;

    @Schema(description = "투표 마감 시간", example = "2024-12-31T23:59:59", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime endTime;

    @Schema(description = "재투표 허용 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean allowRevote;

    @Schema(description = "조회수", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
    private int viewCount;

    @Schema(description = "댓글 수", example = "45", requiredMode = Schema.RequiredMode.REQUIRED)
    private long commentCount;

    @Schema(description = "생성일시", example = "2024-01-01T10:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createdDate;

    @Schema(description = "수정일시", example = "2024-01-02T15:30:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime lastModifiedDate;
}
