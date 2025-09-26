package com.example.soso.community.common.post.domain.dto;


import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "게시글 생성 응답")
public record PostCreateResponse(
        @Schema(description = "생성된 게시글 ID", example = "101")
        Long postId
) {}