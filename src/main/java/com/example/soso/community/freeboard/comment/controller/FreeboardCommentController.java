package com.example.soso.community.freeboard.comment.controller;

import com.example.soso.community.freeboard.comment.domain.dto.*;
import com.example.soso.community.freeboard.comment.service.FreeboardCommentService;
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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
@Tag(name = "Freeboard Comment", description = "자유게시판 댓글 API")
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
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "빈 내용", value = "{\"code\": \"VALIDATION_FAILED\", \"message\": \"[content] 댓글 내용은 필수입니다.\"}"),
                                    @ExampleObject(name = "댓글 길이 초과", value = "{\"code\": \"COMMENT_CONTENT_TOO_LONG\", \"message\": \"댓글이 너무 깁니다.\"}"),
                                    @ExampleObject(name = "대댓글 제한", value = "{\"code\": \"REPLY_DEPTH_EXCEEDED\", \"message\": \"대댓글의 대댓글은 허용되지 않습니다.\"}")
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "게시글을 찾을 수 없음",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"POST_NOT_FOUND\", \"message\": \"게시글을 찾을 수 없습니다.\"}")
                    )
            )
    })
    @PostMapping
    public ResponseEntity<?> createComment(
            @Parameter(description = "게시글 ID", example = "123")
            @PathVariable Long freeboardId,
            @RequestBody @Valid FreeboardCommentCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            log.warn("자유게시판 댓글 작성 요청 시 인증 정보 없음: freeboardId={}", freeboardId);
            ErrorResponse errorResponse = new ErrorResponse("AUTHENTICATION_FAILED", "인증이 필요합니다.");
            return ResponseEntity.status(401).body(errorResponse);
        }

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
                    content = @Content(
                            schema = @Schema(implementation = FreeboardCommentCursorResponse.class),
                            examples = @ExampleObject(
                                    value = "{\"comments\":[{\"commentId\":456,\"postId\":123,\"parentCommentId\":null,\"author\":{\"userId\":\"commenter1\",\"nickname\":\"댓글러\",\"profileImageUrl\":\"https://cdn.example.com/user.jpg\"},\"content\":\"첫 댓글입니다.\",\"replyCount\":0,\"likeCount\":0,\"isLiked\":false,\"depth\":0,\"deleted\":false,\"isAuthor\":false,\"createdAt\":\"2025-01-01T10:10:00\",\"updatedAt\":\"2025-01-01T10:10:00\"}],\"hasNext\":false,\"nextCursor\":null,\"size\":1}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 파라미터",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "잘못된 정렬 값", value = "{\"code\": \"INVALID_ENUM_VALUE\", \"message\": \"'INVALID_SORT'은(는) 허용되지 않는 값입니다. 사용 가능한 값: [LATEST, OLDEST]\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "게시글을 찾을 수 없음",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"POST_NOT_FOUND\", \"message\": \"게시글을 찾을 수 없습니다.\"}")
                    )
            )
    })
    @GetMapping
    public ResponseEntity<FreeboardCommentCursorResponse> getCommentsByCursor(
            @Parameter(description = "게시글 ID", example = "123")
            @PathVariable Long freeboardId,
            @RequestParam(defaultValue = "LATEST") FreeboardCommentSortType sort,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        // 선택적 인증 처리
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = null;
        if (authentication != null && authentication.isAuthenticated() &&
            !"anonymousUser".equals(authentication.getPrincipal()) &&
            authentication.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            userId = userDetails.getUser().getId();
        }

        log.info("자유게시판 댓글 목록 조회 요청: freeboardId={}, sort={}, cursor={}, size={}, userId={}",
                freeboardId, sort, cursor != null ? "present" : "null", size, userId != null ? userId : "anonymous");

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
            @ApiResponse(
                    responseCode = "200",
                    description = "수정 성공",
                    content = @Content(schema = @Schema(implementation = FreeboardCommentCreateResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "빈 내용", value = "{\"code\": \"VALIDATION_FAILED\", \"message\": \"[content] 댓글 내용은 필수입니다.\"}"),
                                    @ExampleObject(name = "댓글 길이 초과", value = "{\"code\": \"COMMENT_CONTENT_TOO_LONG\", \"message\": \"댓글이 너무 깁니다.\"}")
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
                    responseCode = "403",
                    description = "권한 없음",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"COMMENT_ACCESS_DENIED\", \"message\": \"댓글에 대한 접근 권한이 없습니다.\"}")
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
    @PatchMapping("/{commentId}")
    public ResponseEntity<?> updateComment(
            @Parameter(description = "게시글 ID", example = "123")
            @PathVariable Long freeboardId,
            @Parameter(description = "댓글 ID", example = "456")
            @PathVariable Long commentId,
            @RequestBody @Valid FreeboardCommentUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            log.warn("자유게시판 댓글 수정 요청 시 인증 정보 없음: freeboardId={}, commentId={}", freeboardId, commentId);
            ErrorResponse errorResponse = new ErrorResponse("AUTHENTICATION_FAILED", "인증이 필요합니다.");
            return ResponseEntity.status(401).body(errorResponse);
        }

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
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"AUTHENTICATION_FAILED\", \"message\": \"인증이 필요합니다.\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "삭제 권한 없음",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"COMMENT_ACCESS_DENIED\", \"message\": \"댓글에 대한 접근 권한이 없습니다.\"}")
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
    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(
            @Parameter(description = "게시글 ID", example = "123")
            @PathVariable Long freeboardId,
            @Parameter(description = "댓글 ID", example = "456")
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            log.warn("자유게시판 댓글 삭제 요청 시 인증 정보 없음: freeboardId={}, commentId={}", freeboardId, commentId);
            ErrorResponse errorResponse = new ErrorResponse("AUTHENTICATION_FAILED", "인증이 필요합니다.");
            return ResponseEntity.status(401).body(errorResponse);
        }

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
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"AUTHENTICATION_FAILED\", \"message\": \"인증이 필요합니다.\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "관리자 권한 필요",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"COMMENT_ACCESS_DENIED\", \"message\": \"댓글에 대한 접근 권한이 없습니다.\"}")
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
    @DeleteMapping("/{commentId}/force")
    public ResponseEntity<?> hardDeleteComment(
            @Parameter(description = "게시글 ID", example = "123")
            @PathVariable Long freeboardId,
            @Parameter(description = "댓글 ID", example = "456")
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            log.warn("자유게시판 댓글 영구 삭제 요청 시 인증 정보 없음: freeboardId={}, commentId={}", freeboardId, commentId);
            ErrorResponse errorResponse = new ErrorResponse("AUTHENTICATION_FAILED", "인증이 필요합니다.");
            return ResponseEntity.status(401).body(errorResponse);
        }

        log.warn("자유게시판 댓글 영구 삭제 요청: freeboardId={}, commentId={}, userId={}",
                freeboardId, commentId, userDetails.getUser().getId());

        String userId = userDetails.getUser().getId();
        commentService.hardDeleteComment(freeboardId, commentId, userId);

        log.warn("자유게시판 댓글 영구 삭제 완료: commentId={}", commentId);
        return ResponseEntity.noContent().build();
    }
}
