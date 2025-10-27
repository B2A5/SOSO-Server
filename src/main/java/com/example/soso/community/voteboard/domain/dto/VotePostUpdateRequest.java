package com.example.soso.community.voteboard.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 투표 게시글 수정 요청 DTO
 *
 * 투표 옵션은 수정 불가능 (title, content, images, endTime, allowRevote만 수정 가능)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "투표 게시글 수정 요청 (투표 옵션 수정 불가)")
public class VotePostUpdateRequest {

    @Size(max = 100, message = "제목은 최대 100자까지 입력 가능합니다.")
    @Schema(description = "게시글 제목", example = "우리 동네 카페 선호도 조사 (수정)")
    private String title;

    @Size(max = 5000, message = "내용은 최대 5000자까지 입력 가능합니다.")
    @Schema(description = "게시글 내용", example = "여러분이 가장 좋아하는 카페 스타일은 무엇인가요? (수정)")
    private String content;

    @Size(max = 5, message = "이미지는 최대 5개까지 업로드 가능합니다.")
    @Schema(description = "이미지 URL 목록 (최대 5개)", example = "[\"https://example.com/image1.jpg\"]")
    private List<String> imageUrls;

    @Future(message = "투표 마감 시간은 미래 시간이어야 합니다.")
    @Schema(description = "투표 마감 시간", example = "2024-12-31T23:59:59")
    private LocalDateTime endTime;

    @Schema(description = "재투표 허용 여부", example = "false")
    private Boolean allowRevote;
}
