package com.example.soso.community.pollboard.comment.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "투표 게시판 댓글 작성 요청")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollCommentCreateRequest {

    @Schema(description = "댓글 내용", example = "좋은 투표 주제네요!")
    @NotBlank(message = "댓글 내용은 필수입니다.")
    @Size(max = 1000, message = "댓글은 1000자 이하로 입력해주세요.")
    private String content;

    @Schema(description = "부모 댓글 ID (대댓글인 경우)", example = "789")
    private Long parentCommentId;
}
