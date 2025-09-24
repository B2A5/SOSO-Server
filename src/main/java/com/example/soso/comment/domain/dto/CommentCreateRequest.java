package com.example.soso.comment.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "댓글 작성 요청 DTO")
public record CommentCreateRequest(

        @Schema(description = "댓글 내용", example = "좋은 글 감사합니다!")
        String content

) {

}
