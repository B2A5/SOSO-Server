package com.example.soso.community.common.comment.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "댓글 작성 요청 DTO")
public record CommentCreateRequest(

        @Schema(description = "댓글 내용", example = "좋은 글 감사합니다!", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String content

) {

}
