package com.example.soso.community.pollboard.domain.dto;

import com.example.soso.community.common.post.domain.entity.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 투표 게시판 게시글 생성 요청
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "투표 게시판 게시글 생성 요청")
public class PollCreateRequest {

    @Schema(
            description = "게시글 카테고리",
            example = "restaurant",
            allowableValues = {"daily-hobby", "restaurant", "living-convenience", "neighborhood-news", "startup", "others"}
    )
    @NotNull(message = "카테고리는 필수입니다.")
    private Category category;

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 100, message = "제목은 최대 100자까지 입력 가능합니다.")
    @Schema(description = "게시글 제목", example = "우리 동네 카페 선호도 조사", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    @Size(max = 5000, message = "내용은 최대 5000자까지 입력 가능합니다.")
    @Schema(description = "게시글 내용", example = "여러분이 가장 좋아하는 카페 스타일은 무엇인가요?", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @NotNull(message = "투표 옵션은 필수입니다.")
    @Size(min = 2, max = 5, message = "투표 옵션은 최소 2개, 최대 5개까지 가능합니다.")
    @Valid
    @Schema(description = "투표 옵션 목록 (2-5개)", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<PollOptionRequest> voteOptions;

    @NotNull(message = "투표 마감 시간은 필수입니다.")
    @Future(message = "투표 마감 시간은 미래 시간이어야 합니다.")
    @Schema(description = "투표 마감 시간", example = "2024-12-31T23:59:59", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime endTime;

    @NotNull(message = "재투표 허용 여부는 필수입니다.")
    @Schema(description = "재투표 허용 여부 (투표 후 변경 가능 여부)", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean allowRevote;

    @NotNull(message = "중복 선택 허용 여부는 필수입니다.")
    @Schema(
            description = "중복 선택 허용 여부 (true: 여러 옵션 동시 선택 가능, 최대 n-1개 / false: 하나의 옵션만 선택 가능)",
            example = "false",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Boolean allowMultipleChoice;

    @Schema(
            description = "첨부 이미지 파일들 (최대 4장)",
            type = "array",
            format = "binary"
    )
    @Size(max = 4, message = "이미지는 최대 4장까지 업로드 가능합니다.")
    private List<MultipartFile> images;
}
