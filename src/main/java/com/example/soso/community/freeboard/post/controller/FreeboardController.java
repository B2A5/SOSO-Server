package com.example.soso.community.freeboard.post.controller;

import com.example.soso.community.freeboard.post.domain.dto.*;
import com.example.soso.community.freeboard.post.service.FreeboardService;
import com.example.soso.community.common.post.domain.entity.Category;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 자유게시판 관련 API를 제공하는 컨트롤러
 *
 * 주요 기능:
 * - 게시글 CRUD (생성, 조회, 수정, 삭제)
 * - 커서 기반 게시글 목록 조회
 * - S3 기반 이미지 업로드 (최대 4장)
 */
@Slf4j
@Tag(name = "Freeboard", description = "자유게시판 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/community/freeboard")
public class FreeboardController {

    private final FreeboardService freeboardService;

    @Operation(
            summary = "자유게시판 글 작성",
            description = """
                    자유게시판에 새 글을 작성합니다.

                    **특징:**
                    - 이미지 업로드 지원 (최대 4장)
                    - Multipart 방식으로 이미지와 텍스트 데이터를 함께 전송
                    - 카테고리 필수 선택

                    **지원 파일 형식:** jpg, jpeg, png, gif
                    **최대 파일 크기:** 5MB per image
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(implementation = FreeboardCreateRequest.class)
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "게시글 작성 성공",
                    content = @Content(
                            schema = @Schema(implementation = FreeboardCreateResponse.class),
                            examples = @ExampleObject(value = "{\"postId\": 123}")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(
                            examples = {
                                    @ExampleObject(name = "빈 제목", value = "{\"code\": \"INVALID_INPUT\", \"message\": \"제목은 필수입니다.\"}"),
                                    @ExampleObject(name = "이미지 초과", value = "{\"code\": \"TOO_MANY_IMAGES\", \"message\": \"이미지는 최대 4장까지 업로드 가능합니다.\"}")
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(examples = @ExampleObject(value = "{\"code\": \"UNAUTHORIZED\", \"message\": \"로그인이 필요합니다.\"}"))
            ),
            @ApiResponse(
                    responseCode = "413",
                    description = "파일 크기 초과",
                    content = @Content(examples = @ExampleObject(value = "{\"code\": \"FILE_TOO_LARGE\", \"message\": \"파일 크기가 너무 큽니다. (최대 5MB)\"}"))
            )
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FreeboardCreateResponse> createPost(
            @ModelAttribute @Valid FreeboardCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("자유게시판 글 작성 요청: userId={}, category={}, title={}, imageCount={}",
                userDetails.getUser().getId(),
                request.getCategory(),
                request.getTitle(),
                request.getImages() != null ? request.getImages().size() : 0);

        String userId = userDetails.getUser().getId();
        FreeboardCreateResponse response = freeboardService.createPost(request, userId);

        log.info("자유게시판 글 작성 완료: postId={}", response.getPostId());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "자유게시판 글 상세 조회",
            description = "게시글 ID를 통해 특정 게시글의 상세 정보를 조회합니다. 조회 시 조회수가 증가합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = FreeboardDetailResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "게시글을 찾을 수 없음",
                    content = @Content(examples = @ExampleObject(value = "{\"code\": \"POST_NOT_FOUND\", \"message\": \"게시글을 찾을 수 없습니다.\"}"))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패"
            )
    })
    @GetMapping("/{freeboardId}")
    public ResponseEntity<FreeboardDetailResponse> getPost(
            @Parameter(description = "조회할 게시글 ID", example = "123")
            @PathVariable Long freeboardId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("자유게시판 글 조회 요청: freeboardId={}, userId={}",
                freeboardId, userDetails.getUser().getId());

        String userId = userDetails.getUser().getId();
        FreeboardDetailResponse response = freeboardService.getPost(freeboardId, userId);

        log.debug("자유게시판 글 조회 완료: freeboardId={}, viewCount={}",
                freeboardId, response.getViewCount());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "자유게시판 글 목록 조회 (커서 기반)",
            description = """
                    커서 기반 페이지네이션을 사용하여 게시글 목록을 조회합니다.

                    **정렬 옵션:**
                    - LATEST: 최신순 (기본값)
                    - LIKE: 좋아요순
                    - COMMENT: 댓글순
                    - VIEW: 조회순

                    **커서 사용법:**
                    첫 요청: cursor 없이 요청
                    다음 페이지: 이전 응답의 nextCursor 값을 사용
                    """,
            parameters = {
                    @Parameter(
                            name = "category",
                            description = "필터링할 카테고리",
                            example = "restaurant"
                    ),
                    @Parameter(
                            name = "sort",
                            description = "정렬 기준",
                            example = "LATEST"
                    ),
                    @Parameter(
                            name = "cursor",
                            description = "커서 값 (다음 페이지를 위한)",
                            example = "eyJpZCI6MTIzLCJzb3J0VmFsdWUiOiIyMDI0LTEyLTI1VDEwOjAwOjAwIn0="
                    ),
                    @Parameter(
                            name = "size",
                            description = "페이지 크기 (1-50)",
                            example = "10"
                    )
            }
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 파라미터"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping
    public ResponseEntity<FreeboardCursorResponse> getPostsByCursor(
            @RequestParam(required = false) Category category,
            @RequestParam(defaultValue = "LATEST") FreeboardSortType sort,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("자유게시판 목록 조회 요청: category={}, sort={}, cursor={}, size={}, userId={}",
                category, sort, cursor != null ? "present" : "null", size, userDetails.getUser().getId());

        String userId = userDetails.getUser().getId();
        FreeboardCursorResponse response = freeboardService.getPostsByCursor(category, sort, size, cursor, userId);

        log.debug("자유게시판 목록 조회 완료: resultCount={}, hasNext={}",
                response.getPosts().size(), response.isHasNext());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "자유게시판 글 수정",
            description = """
                    작성한 게시글을 수정합니다.

                    **수정 가능 항목:**
                    - 제목
                    - 내용
                    - 카테고리
                    - 이미지 (기존 이미지 교체 가능)

                    **권한:** 작성자 본인만 수정 가능
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음",
                    content = @Content(examples = @ExampleObject(value = "{\"code\": \"ACCESS_DENIED\", \"message\": \"수정 권한이 없습니다.\"}"))
            ),
            @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    })
    @PatchMapping(value = "/{freeboardId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FreeboardCreateResponse> updatePost(
            @Parameter(description = "수정할 게시글 ID", example = "123")
            @PathVariable Long freeboardId,
            @ModelAttribute @Valid FreeboardUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("자유게시판 글 수정 요청: freeboardId={}, userId={}",
                freeboardId, userDetails.getUser().getId());

        String userId = userDetails.getUser().getId();
        FreeboardCreateResponse response = freeboardService.updatePost(freeboardId, request, userId);

        log.info("자유게시판 글 수정 완료: freeboardId={}", response.getPostId());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "자유게시판 글 삭제 (소프트 삭제)",
            description = """
                    게시글을 삭제합니다. (소프트 삭제)

                    **삭제 특징:**
                    - 실제로는 deleted 플래그만 true로 변경
                    - 댓글 및 좋아요 정보는 보존
                    - 관리자는 복구 가능

                    **권한:** 작성자 본인만 삭제 가능
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "삭제 권한 없음"),
            @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    })
    @DeleteMapping("/{freeboardId}")
    public ResponseEntity<Void> deletePost(
            @Parameter(description = "삭제할 게시글 ID", example = "123")
            @PathVariable Long freeboardId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("자유게시판 글 삭제 요청: freeboardId={}, userId={}",
                freeboardId, userDetails.getUser().getId());

        String userId = userDetails.getUser().getId();
        freeboardService.deletePost(freeboardId, userId);

        log.info("자유게시판 글 삭제 완료: freeboardId={}", freeboardId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "자유게시판 글 영구 삭제",
            description = """
                    게시글을 영구적으로 삭제합니다. (하드 삭제)

                    **주의사항:**
                    - 복구 불가능한 삭제
                    - 관련된 댓글, 좋아요, 이미지 모두 삭제
                    - 관리자 권한 필요
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "영구 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 필요"),
            @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    })
    @DeleteMapping("/{freeboardId}/force")
    public ResponseEntity<Void> hardDeletePost(
            @Parameter(description = "영구 삭제할 게시글 ID", example = "123")
            @PathVariable Long freeboardId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.warn("자유게시판 글 영구 삭제 요청: freeboardId={}, userId={}",
                freeboardId, userDetails.getUser().getId());

        String userId = userDetails.getUser().getId();
        freeboardService.hardDeletePost(freeboardId, userId);

        log.warn("자유게시판 글 영구 삭제 완료: freeboardId={}", freeboardId);
        return ResponseEntity.noContent().build();
    }
}