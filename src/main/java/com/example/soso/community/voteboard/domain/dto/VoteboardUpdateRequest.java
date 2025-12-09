package com.example.soso.community.voteboard.domain.dto;

import com.example.soso.community.common.post.domain.entity.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 투표 게시판 게시글 수정 요청
 *
 * 투표 옵션은 수정 불가능 (title, content, category, images, endTime, allowRevote만 수정 가능)
 */
@Getter
@Setter
@Schema(description = "투표 게시판 게시글 수정 요청 (투표 옵션 수정 불가)")
public class VoteboardUpdateRequest {

    @Schema(
            description = "수정할 카테고리",
            example = "restaurant",
            allowableValues = {"daily-hobby", "restaurant", "living-convenience", "neighborhood-news", "startup", "others"}
    )
    private Category category;

    @Size(max = 100, message = "제목은 최대 100자까지 입력 가능합니다.")
    @Schema(description = "게시글 제목", example = "우리 동네 카페 선호도 조사 (수정)")
    private String title;

    @Size(max = 5000, message = "내용은 최대 5000자까지 입력 가능합니다.")
    @Schema(description = "게시글 내용", example = "여러분이 가장 좋아하는 카페 스타일은 무엇인가요? (수정)")
    private String content;

    @Schema(description = "새로운 이미지 파일들 (기존 이미지 대체, 최대 4장)")
    @Size(max = 4, message = "이미지는 최대 4장까지 업로드 가능합니다.")
    private List<MultipartFile> images;

    @Schema(description = "삭제할 기존 이미지 ID 목록")
    private List<Long> deleteImageIds;

    @Future(message = "투표 마감 시간은 미래 시간이어야 합니다.")
    @Schema(description = "투표 마감 시간", example = "2024-12-31T23:59:59")
    private LocalDateTime endTime;

    @Schema(description = "재투표 허용 여부 (투표 후 변경 가능 여부)", example = "false")
    private Boolean allowRevote;

    @Schema(description = "중복 선택 허용 여부 (여러 옵션 동시 선택 가능 여부)", example = "false")
    private Boolean allowMultipleChoice;
}
