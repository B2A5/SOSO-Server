package com.example.soso.community.common.post.domain.dto;

import com.example.soso.community.common.post.domain.entity.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

@Schema(description = "게시글 수정 요청 DTO")
public record PostUpdateRequest(

        @Schema(description = "수정할 제목", example = "제목 수정 예시", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String title,

        @Schema(description = "수정할 본문", example = "내용 수정 예시", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String content,

        @Schema(description = "수정할 카테고리", example = "ANNOUNCEMENT", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Category category,

        @Schema(description = "새 이미지 파일 목록 (선택)", type = "array", implementation = MultipartFile.class, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        List<MultipartFile> images

) {}
