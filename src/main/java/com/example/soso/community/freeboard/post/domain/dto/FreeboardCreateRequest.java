package com.example.soso.community.freeboard.post.domain.dto;

import com.example.soso.community.common.post.domain.entity.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Schema(description = "자유게시판 글 작성 요청")
@Getter
@Setter
public class FreeboardCreateRequest {

    @Schema(
            description = "게시글 카테고리",
            example = "restaurant",
            allowableValues = {"daily-hobby", "restaurant", "living-convenience", "neighborhood-news", "startup", "others"}
    )
    @NotNull(message = "카테고리는 필수입니다.")
    private Category category;

    @Schema(description = "게시글 제목", example = "맛있는 라면집 추천해요!")
    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 100, message = "제목은 100자 이하로 입력해주세요.")
    private String title;

    @Schema(description = "게시글 내용", example = "어제 갔던 라면집이 정말 맛있어서 공유합니다...")
    @NotBlank(message = "내용은 필수입니다.")
    @Size(max = 5000, message = "내용은 5000자 이하로 입력해주세요.")
    private String content;

    @Schema(
            description = "첨부 이미지 파일들 (최대 4장)",
            type = "array",
            format = "binary"
    )
    @Size(max = 4, message = "이미지는 최대 4장까지 업로드 가능합니다.")
    private List<MultipartFile> images;
}