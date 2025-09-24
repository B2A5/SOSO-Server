package com.example.soso.community.freeboard.domain.dto;

import com.example.soso.post.domain.entity.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Schema(description = "자유게시판 글 수정 요청")
@Getter
@Setter
public class FreeboardUpdateRequest {

    @Schema(
            description = "수정할 카테고리",
            example = "restaurant",
            allowableValues = {"daily-hobby", "restaurant", "living-convenience", "neighborhood-news", "startup", "others"}
    )
    private Category category;

    @Schema(description = "수정할 제목", example = "수정된 맛집 정보!")
    @Size(max = 100, message = "제목은 100자 이하로 입력해주세요.")
    private String title;

    @Schema(description = "수정할 내용", example = "추가 정보를 업데이트합니다...")
    @Size(max = 5000, message = "내용은 5000자 이하로 입력해주세요.")
    private String content;

    @Schema(description = "새로운 이미지 파일들 (기존 이미지 대체, 최대 4장)")
    @Size(max = 4, message = "이미지는 최대 4장까지 업로드 가능합니다.")
    private List<MultipartFile> images;

    @Schema(description = "삭제할 기존 이미지 ID 목록")
    private List<Long> deleteImageIds;
}