package com.example.soso.community.voteboard.comment.controller;

import com.example.soso.community.voteboard.comment.service.VoteboardCommentLikeService;
import com.example.soso.security.domain.CustomUserDetails;
import com.example.soso.global.exception.domain.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 투표 게시판 댓글 좋아요 API 컨트롤러
 */
@Tag(name = "Voteboard Comment Like", description = "투표 게시판 댓글 좋아요 API")
@Slf4j
@RestController
@RequestMapping("/community/votesboard/{votePostId}/comments/{commentId}/like")
@RequiredArgsConstructor
public class VoteboardCommentLikeController {

    private final VoteboardCommentLikeService commentLikeService;

    @Operation(
            operationId = "toggleVoteboardCommentLike",
            summary = "댓글 좋아요 토글",
            description = """
                    댓글에 좋아요를 추가하거나 취소합니다.

                    **동작 방식:**
                    - 이미 좋아요한 댓글이면 좋아요를 취소하고 false 반환
                    - 좋아요하지 않은 댓글이면 좋아요를 추가하고 true 반환

                    **인증:** 로그인 필수
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "좋아요 토글 성공",
                    content = @Content(
                            examples = {
                                    @ExampleObject(name = "좋아요 추가", value = "true"),
                                    @ExampleObject(name = "좋아요 취소", value = "false")
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"AUTHENTICATION_FAILED\", \"message\": \"인증이 필요합니다.\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "댓글을 찾을 수 없음",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"COMMENT_NOT_FOUND\", \"message\": \"댓글을 찾을 수 없습니다.\"}")
                    )
            )
    })
    @PostMapping
    public ResponseEntity<Object> toggleLike(
            @Parameter(description = "투표 게시글 ID", example = "123")
            @PathVariable Long votePostId,
            @Parameter(description = "댓글 ID", example = "456")
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            log.warn("toggleLike 요청 시 인증 정보 없음: commentId={}", commentId);
            ErrorResponse errorResponse = new ErrorResponse("AUTHENTICATION_FAILED", "인증이 필요합니다.");
            return ResponseEntity.status(401).body(errorResponse);
        }

        log.info("투표게시판 댓글 좋아요 토글: commentId={}, userId={}", commentId, userDetails.getUsername());

        boolean isLiked = commentLikeService.toggleLike(commentId, userDetails.getUsername());

        return ResponseEntity.ok(isLiked);
    }

    @Operation(
            operationId = "getVoteboardCommentLikeStatus",
            summary = "댓글 좋아요 상태 확인",
            description = """
                    사용자가 특정 댓글에 좋아요를 했는지 확인합니다.

                    **인증:** 로그인 필수
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "좋아요 상태 조회 성공",
                    content = @Content(
                            examples = {
                                    @ExampleObject(name = "좋아요 한 상태", value = "true"),
                                    @ExampleObject(name = "좋아요 안 한 상태", value = "false")
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"AUTHENTICATION_FAILED\", \"message\": \"인증이 필요합니다.\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "댓글을 찾을 수 없음",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"COMMENT_NOT_FOUND\", \"message\": \"댓글을 찾을 수 없습니다.\"}")
                    )
            )
    })
    @GetMapping
    public ResponseEntity<Object> getLikeStatus(
            @Parameter(description = "투표 게시글 ID", example = "123")
            @PathVariable Long votePostId,
            @Parameter(description = "댓글 ID", example = "456")
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            log.warn("getLikeStatus 요청 시 인증 정보 없음: commentId={}", commentId);
            ErrorResponse errorResponse = new ErrorResponse("AUTHENTICATION_FAILED", "인증이 필요합니다.");
            return ResponseEntity.status(401).body(errorResponse);
        }

        log.debug("투표게시판 댓글 좋아요 상태 조회: commentId={}, userId={}", commentId, userDetails.getUsername());

        boolean isLiked = commentLikeService.isLikedByUser(commentId, userDetails.getUsername());

        return ResponseEntity.ok(isLiked);
    }
}
