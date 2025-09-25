package com.example.soso.community.freeboard.comment.controller;

import com.example.soso.community.freeboard.comment.service.FreeboardCommentLikeService;
import com.example.soso.security.domain.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 자유게시판 댓글 좋아요 API 컨트롤러
 */
@Tag(name = "Freeboard Comment Like", description = "자유게시판 댓글 좋아요 API")
@Slf4j
@RestController
@RequestMapping("/community/freeboard/{freeboardId}/comments/{commentId}/like")
@RequiredArgsConstructor
public class FreeboardCommentLikeController {

    private final FreeboardCommentLikeService commentLikeService;

    @Operation(
            summary = "자유게시판 댓글 좋아요 토글",
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
                    description = "댓글 좋아요 토글 성공",
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
                    content = @Content(examples = @ExampleObject(value = "{\"code\": \"UNAUTHORIZED\", \"message\": \"로그인이 필요합니다.\"}"))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "게시글 또는 댓글을 찾을 수 없음",
                    content = @Content(examples = @ExampleObject(value = "{\"code\": \"NOT_FOUND\", \"message\": \"댓글을 찾을 수 없습니다.\"}"))
            )
    })
    @PostMapping
    public ResponseEntity<Boolean> toggleCommentLike(
            @Parameter(description = "자유게시판 게시글 ID", example = "123")
            @PathVariable Long freeboardId,
            @Parameter(description = "댓글 ID", example = "456")
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            log.warn("toggleCommentLike 요청 시 인증 정보 없음: freeboardId={}, commentId={}", freeboardId, commentId);
            return ResponseEntity.status(401).build();
        }

        log.info("자유게시판 댓글 좋아요 토글: freeboardId={}, commentId={}, userId={}",
                freeboardId, commentId, userDetails.getUsername());

        boolean isLiked = commentLikeService.toggleCommentLike(freeboardId, commentId, userDetails.getUsername());

        return ResponseEntity.ok(isLiked);
    }

    @Operation(
            summary = "자유게시판 댓글 좋아요 상태 확인",
            description = """
                    사용자가 특정 댓글에 좋아요를 했는지 확인합니다.

                    **인증:** 로그인 필수
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "댓글 좋아요 상태 조회 성공",
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
                    content = @Content(examples = @ExampleObject(value = "{\"code\": \"UNAUTHORIZED\", \"message\": \"로그인이 필요합니다.\"}"))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "게시글 또는 댓글을 찾을 수 없음",
                    content = @Content(examples = @ExampleObject(value = "{\"code\": \"NOT_FOUND\", \"message\": \"댓글을 찾을 수 없습니다.\"}"))
            )
    })
    @GetMapping
    public ResponseEntity<Boolean> getCommentLikeStatus(
            @Parameter(description = "자유게시판 게시글 ID", example = "123")
            @PathVariable Long freeboardId,
            @Parameter(description = "댓글 ID", example = "456")
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            log.warn("getCommentLikeStatus 요청 시 인증 정보 없음: freeboardId={}, commentId={}", freeboardId, commentId);
            return ResponseEntity.status(401).build();
        }

        log.debug("자유게시판 댓글 좋아요 상태 조회: freeboardId={}, commentId={}, userId={}",
                freeboardId, commentId, userDetails.getUsername());

        boolean isLiked = commentLikeService.isCommentLikedByUser(freeboardId, commentId, userDetails.getUsername());

        return ResponseEntity.ok(isLiked);
    }
}