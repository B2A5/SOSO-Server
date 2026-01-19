package com.example.soso.community.votesboard.comment.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "투표 게시판 댓글 수정 요청")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VotesboardCommentUpdateRequest {

    @Schema(description = "댓글 내용", example = "수정된 댓글 내용입니다")
    @NotBlank(message = "댓글 내용은 필수입니다.")
    @Size(max = 1000, message = "댓글은 1000자 이하로 입력해주세요.")
    private String content;
}
