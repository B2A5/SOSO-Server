package com.example.soso.community.votesboard.comment.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "투표 게시판 댓글 작성/수정 응답")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class VoteboardCommentCreateResponse {

    @Schema(description = "생성/수정된 댓글 ID", example = "456", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long commentId;
}
