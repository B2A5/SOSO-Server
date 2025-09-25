package com.example.soso.community.freeboard.comment.controller;

import com.example.soso.community.freeboard.comment.domain.dto.*;
import com.example.soso.community.freeboard.comment.service.FreeboardCommentService;
import com.example.soso.security.domain.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 자유게시판 댓글 관련 API를 제공하는 컨트롤러
 *
 * 주요 기능:
 * - 댓글 작성, 조회, 수정, 삭제
 * - 커서 기반 댓글 목록 조회 (최신순/오래된순)
 * - 대댓글 지원
 */
@Slf4j
@Tag(name = "Freeboard Comments", description = "자유게시판 댓글 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/community/freeboard/{freeboardId}/comments")
public class FreeboardCommentController {

    private final FreeboardCommentService commentService;

    @Operation(
            summary = "댓글 작성",
            description = """
                    자유게시판 게시글에 댓글을 작성합니다.

                    **특징:**
                    - 일반 댓글과 대댓글 지원
                    - 대댓글 작성 시 parentCommentId 필수
                    - 익명 댓글 불가 (로그인 필수)
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "댓글 작성 요청",
                    content = @Content(schema = @Schema(implementation = FreeboardCommentCreateRequest.class))
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "댓글 작성 성공",
                    content = @Content(
                            schema = @Schema(implementation = FreeboardCommentCreateResponse.class),
                            examples = @ExampleObject(value = "{\"commentId\": 456}")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(examples = {
                            @ExampleObject(name = "빈 내용", value = "{\"code\": \"INVALID_INPUT\", \"message\": \"댓글 내용은 필수입니다.\"}"),
                            @ExampleObject(name = "부모 댓글 없음", value = "{\"code\": \"PARENT_COMMENT_NOT_FOUND\", \"message\": \"부모 댓글을 찾을 수 없습니다.\"}")
                    })
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "게시글을 찾을 수 없음"
            )
    })
    @PostMapping
    public ResponseEntity<FreeboardCommentCreateResponse> createComment(
            @Parameter(description = "게시글 ID", example = "123")
            @PathVariable Long freeboardId,
            @RequestBody @Valid FreeboardCommentCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("자유게시판 댓글 작성 요청: freeboardId={}, userId={}, parentCommentId={}",
                freeboardId, userDetails.getUser().getId(), request.getParentCommentId());

        String userId = userDetails.getUser().getId();
        FreeboardCommentCreateResponse response = commentService.createComment(freeboardId, request, userId);

        log.info("자유게시판 댓글 작성 완료: commentId={}", response.getCommentId());
        return ResponseEntity.status(201).body(response);
    }

    @Operation(
            summary = "댓글 목록 조회 (커서 기반)",
            description = """
                    게시글의 댓글 목록을 커서 기반으로 조회합니다.

                    **정렬 옵션:**
                    - LATEST: 최신순 (기본값)
                    - OLDEST: 오래된순

                    **특징:**
                    - 계층 구조 지원 (부모 댓글 → 대댓글)
                    - 삭제된 댓글은 "삭제된 댓글입니다" 표시
                    - 작성자 정보 포함
                    """,
            parameters = {
                    @Parameter(
                            name = "sort",
                            description = "정렬 순서",
                            example = "LATEST"
                    ),
                    @Parameter(
                            name = "cursor",
                            description = "커서 값 (다음 페이지용)",
                            example = "eyJpZCI6NDU2LCJzb3J0VmFsdWUiOiIyMDI0LTEyLTI1VDEwOjAwOjAwIn0="
                    ),
                    @Parameter(
                            name = "size",
                            description = "페이지 크기 (1-50)",
                            example = "20"
                    )
            }
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = FreeboardCommentCursorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 파라미터"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "게시글을 찾을 수 없음"
            )
    })
    @GetMapping
    public ResponseEntity<FreeboardCommentCursorResponse> getCommentsByCursor(
            @Parameter(description = "게시글 ID", example = "123")
            @PathVariable Long freeboardId,
            @RequestParam(defaultValue = "LATEST") FreeboardCommentSortType sort,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("자유게시판 댓글 목록 조회 요청: freeboardId={}, sort={}, cursor={}, size={}, userId={}",
                freeboardId, sort, cursor != null ? "present" : "null", size, userDetails.getUser().getId());

        String userId = userDetails.getUser().getId();
        FreeboardCommentCursorResponse response = commentService.getCommentsByCursor(freeboardId, sort, size, cursor, userId);

        log.debug("자유게시판 댓글 목록 조회 완료: resultCount={}, hasNext={}",
                response.getComments().size(), response.isHasNext());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "댓글 수정",
            description = """
                    작성한 댓글을 수정합니다.

                    **수정 제한사항:**
                    - 작성자 본인만 수정 가능
                    - 삭제된 댓글은 수정 불가
                    - 내용만 수정 가능 (대댓글 관계는 변경 불가)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음",
                    content = @Content(examples = @ExampleObject(value = "{\"code\": \"ACCESS_DENIED\", \"message\": \"댓글 수정 권한이 없습니다.\"}"))
            ),
            @ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음")
    })
    @PatchMapping("/{commentId}")
    public ResponseEntity<FreeboardCommentCreateResponse> updateComment(
            @Parameter(description = "게시글 ID", example = "123")
            @PathVariable Long freeboardId,
            @Parameter(description = "댓글 ID", example = "456")
            @PathVariable Long commentId,
            @RequestBody @Valid FreeboardCommentUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("자유게시판 댓글 수정 요청: freeboardId={}, commentId={}, userId={}",
                freeboardId, commentId, userDetails.getUser().getId());

        String userId = userDetails.getUser().getId();
        FreeboardCommentCreateResponse response = commentService.updateComment(freeboardId, commentId, request, userId);

        log.info("자유게시판 댓글 수정 완료: commentId={}", response.getCommentId());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "댓글 삭제 (소프트 삭제)",
            description = """
                    댓글을 삭제합니다. (소프트 삭제)

                    **삭제 특징:**
                    - 실제로는 deleted 플래그만 true로 변경
                    - 삭제된 댓글은 "삭제된 댓글입니다" 표시
                    - 대댓글이 있는 부모 댓글도 삭제 가능
                    - 작성자 본인만 삭제 가능
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "삭제 권한 없음"),
            @ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음")
    })
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @Parameter(description = "게시글 ID", example = "123")
            @PathVariable Long freeboardId,
            @Parameter(description = "댓글 ID", example = "456")
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("자유게시판 댓글 삭제 요청: freeboardId={}, commentId={}, userId={}",
                freeboardId, commentId, userDetails.getUser().getId());

        String userId = userDetails.getUser().getId();
        commentService.deleteComment(freeboardId, commentId, userId);

        log.info("자유게시판 댓글 삭제 완료: commentId={}", commentId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "댓글 영구 삭제",
            description = """
                    댓글을 영구적으로 삭제합니다. (하드 삭제)

                    **주의사항:**
                    - 복구 불가능한 삭제
                    - 관련된 대댓글도 모두 삭제
                    - 관리자 권한 필요
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "영구 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 필요"),
            @ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음")
    })
    @DeleteMapping("/{commentId}/force")
    public ResponseEntity<Void> hardDeleteComment(
            @Parameter(description = "게시글 ID", example = "123")
            @PathVariable Long freeboardId,
            @Parameter(description = "댓글 ID", example = "456")
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.warn("자유게시판 댓글 영구 삭제 요청: freeboardId={}, commentId={}, userId={}",
                freeboardId, commentId, userDetails.getUser().getId());

        String userId = userDetails.getUser().getId();
        commentService.hardDeleteComment(freeboardId, commentId, userId);

        log.warn("자유게시판 댓글 영구 삭제 완료: commentId={}", commentId);
        return ResponseEntity.noContent().build();
    }
}