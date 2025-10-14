package com.example.soso.community.freeboard.post.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "자유게시판 글 작성/수정 응답")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FreeboardCreateResponse {

    @Schema(description = "생성/수정된 게시글 ID", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long postId;
}