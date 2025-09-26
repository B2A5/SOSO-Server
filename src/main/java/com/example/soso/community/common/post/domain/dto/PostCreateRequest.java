package com.example.soso.community.common.post.domain.dto;

import com.example.soso.community.common.post.domain.entity.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Schema(description = "게시글 작성 요청 DTO")
public record PostCreateRequest(

        @Schema(description = "게시글 제목", example = "우리 동네 중고거래 팁 공유")
        String title,

        @Schema(description = "게시글 본문", example = "우리 동네에서 제일 인기 있는 거래 장소는...")
        String content,

        @Schema(description = "카테고리", example = "COMMUNITY")
        Category category,

        @Schema(description = "업로드할 이미지 목록", type = "array", implementation = MultipartFile.class)
        List<MultipartFile> images

) {}
