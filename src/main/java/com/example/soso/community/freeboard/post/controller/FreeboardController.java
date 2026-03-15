package com.example.soso.community.freeboard.post.controller;

import com.example.soso.community.freeboard.post.domain.dto.*;
import com.example.soso.community.freeboard.post.service.FreeboardService;
import com.example.soso.community.common.post.domain.entity.Category;

import com.example.soso.security.domain.CustomUserDetails;
import com.example.soso.global.exception.domain.ErrorResponse;
import com.example.soso.global.exception.util.PostException;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
            operationId = "createFreeboardPost",
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
                    responseCode = "201",
                    description = "게시글 작성 성공",
                    content = @Content(
                            schema = @Schema(implementation = FreeboardCreateResponse.class),
                            examples = @ExampleObject(value = "{\"postId\": 123}")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (검증 실패, 이미지 관련 오류)",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "빈 제목", value = "{\"code\": \"VALIDATION_FAILED\", \"message\": \"[title] 제목은 필수입니다.\"}"),
                                    @ExampleObject(name = "빈 내용", value = "{\"code\": \"VALIDATION_FAILED\", \"message\": \"[content] 내용은 필수입니다.\"}"),
                                    @ExampleObject(name = "잘못된 카테고리", value = "{\"code\": \"INVALID_ENUM_VALUE\", \"message\": \"'invalid-category'은(는) 허용되지 않는 값입니다. 사용 가능한 값: [daily-hobby, restaurant, living-convenience, neighborhood-news, startup, others]\"}"),
                                    @ExampleObject(name = "이미지 개수 초과", value = "{\"code\": \"ILLEGAL_ARGUMENT\", \"message\": \"이미지는 최대 4장까지 업로드 가능합니다.\"}"),
                                    @ExampleObject(name = "지원하지 않는 파일 형식", value = "{\"code\": \"ILLEGAL_ARGUMENT\", \"message\": \"지원하지 않는 파일 형식입니다. 지원 형식: image/jpeg, image/jpg, image/png, image/gif, image/webp\"}"),
                                    @ExampleObject(name = "빈 파일", value = "{\"code\": \"ILLEGAL_ARGUMENT\", \"message\": \"이미지 파일이 비어있습니다.\"}")
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
                    responseCode = "413",
                    description = "파일 크기 초과 (Spring multipart 제한)",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"FILE_SIZE_EXCEEDED\", \"message\": \"파일 크기가 너무 큽니다. 최대 업로드 크기는 5MB입니다.\"}")
                    )
            )
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Object> createPost(
            @ModelAttribute @Valid FreeboardCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            log.warn("자유게시판 글 작성 요청 시 인증 정보 없음");
            ErrorResponse errorResponse = new ErrorResponse("AUTHENTICATION_FAILED", "인증이 필요합니다.");
            return ResponseEntity.status(401).body(errorResponse);
        }

        log.info("자유게시판 글 작성 요청: userId={}, category={}, title={}, imageCount={}",
                userDetails.getUser().getId(),
                request.getCategory(),
                request.getTitle(),
                request.getImages() != null ? request.getImages().size() : 0);

        String userId = userDetails.getUser().getId();
        FreeboardCreateResponse response = freeboardService.createPost(request, userId);

        log.info("자유게시판 글 작성 완료: postId={}", response.getPostId());
        return ResponseEntity.status(201).body(response);
    }

    @Operation(
            operationId = "getFreeboardPost",
            summary = "자유게시판 글 상세 조회",
            description = """
                    게시글 ID를 통해 특정 게시글의 상세 정보를 조회합니다.

                    **특징:**
                    - 조회 시 조회수 자동 증가
                    - 인증/비인증 사용자 모두 조회 가능
                    - 인증 여부에 따라 isLiked, canEdit, canDelete 값 변경

                    **인증 사용자:**
                    - isAuthorized: true
                    - isLiked: boolean (좋아요 상태)
                    - canEdit: boolean (수정 권한)
                    - canDelete: boolean (삭제 권한)

                    **비인증 사용자:**
                    - isAuthorized: false
                    - isLiked: null
                    - canEdit: null
                    - canDelete: null
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = FreeboardDetailResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "인증 사용자 (본인 작성)",
                                            value = "{\"postId\":123,\"author\":{\"userId\":\"author123\",\"nickname\":\"작성자\",\"profileImageUrl\":\"https://cdn.example.com/profile.jpg\",\"userType\":\"INHABITANT\",\"address\":\"서울시 강남구\"},\"category\":\"restaurant\",\"title\":\"맛있는 라면집 추천해요!\",\"content\":\"인증 사용자가 작성한 게시글입니다.\",\"images\":[{\"imageId\":1,\"imageUrl\":\"https://cdn.example.com/image1.jpg\",\"sequence\":0}],\"likeCount\":10,\"commentCount\":3,\"viewCount\":120,\"createdAt\":\"2025-01-01T10:00:00\",\"updatedAt\":\"2025-01-02T09:30:00\",\"isAuthorized\":true,\"isAuthor\":true,\"isLiked\":false,\"canEdit\":true,\"canDelete\":true}"
                                    ),
                                    @ExampleObject(
                                            name = "인증 사용자 (다른 사람 글, 좋아요 누름)",
                                            value = "{\"postId\":456,\"author\":{\"userId\":\"other-user\",\"nickname\":\"다른사용자\",\"profileImageUrl\":\"https://cdn.example.com/profile2.jpg\",\"userType\":\"FOUNDER\",\"address\":\"서울시 서초구\"},\"category\":\"startup\",\"title\":\"창업 성공 스토리\",\"content\":\"3개월만에 매출 10배 증가!\",\"images\":[],\"likeCount\":50,\"commentCount\":15,\"viewCount\":500,\"createdAt\":\"2025-01-05T14:20:00\",\"updatedAt\":\"2025-01-06T10:15:00\",\"isAuthorized\":true,\"isAuthor\":false,\"isLiked\":true,\"canEdit\":false,\"canDelete\":false}"
                                    ),
                                    @ExampleObject(
                                            name = "비인증 사용자",
                                            value = "{\"postId\":123,\"author\":{\"userId\":\"author123\",\"nickname\":\"작성자\",\"profileImageUrl\":\"https://cdn.example.com/profile.jpg\",\"userType\":\"INHABITANT\",\"address\":\"서울시 강남구\"},\"category\":\"restaurant\",\"title\":\"맛있는 라면집 추천해요!\",\"content\":\"비인증 사용자가 조회한 게시글입니다.\",\"images\":[],\"likeCount\":10,\"commentCount\":3,\"viewCount\":120,\"createdAt\":\"2025-01-01T10:00:00\",\"isAuthorized\":false,\"isAuthor\":false,\"isLiked\":null,\"canEdit\":null,\"canDelete\":null}"
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "게시글을 찾을 수 없음",
                    content = @Content(examples = @ExampleObject(value = "{\"code\": \"POST_NOT_FOUND\", \"message\": \"게시글을 찾을 수 없습니다.\"}"))
            )
    })
    @GetMapping("/{freeboardId}")
    public ResponseEntity<FreeboardDetailResponse> getPost(
            @Parameter(description = "조회할 게시글 ID", example = "123")
            @PathVariable Long freeboardId
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

        log.info("자유게시판 글 조회 요청: freeboardId={}, userId={}",
                freeboardId, userId != null ? userId : "anonymous");

        FreeboardDetailResponse response = freeboardService.getPost(freeboardId, userId);

        log.debug("자유게시판 글 조회 완료: freeboardId={}, viewCount={}",
                freeboardId, response.getViewCount());
        return ResponseEntity.ok(response);
    }

    @Operation(
            operationId = "getFreeboardPostsByCursor",
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
                            description = """
                                    필터링할 카테고리 (선택사항)

                                    **사용 가능한 값:**
                                    - daily-hobby: 일상/취미
                                    - restaurant: 맛집
                                    - living-convenience: 생활/꿀팁
                                    - neighborhood-news: 동네소식
                                    - startup: 창업
                                    - others: 기타

                                    **미입력 시:** 전체 게시글 조회
                                    """,
                            example = "restaurant",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(
                                    allowableValues = {
                                            "daily-hobby", "restaurant", "living-convenience",
                                            "neighborhood-news", "startup", "others"
                                    }
                            )
                    ),
                    @Parameter(
                            name = "sort",
                            description = """
                                    정렬 기준

                                    - LATEST: 최신순 (기본값)
                                    - LIKE: 좋아요순
                                    - COMMENT: 댓글순
                                    - VIEW: 조회순
                                    """,
                            example = "LATEST"
                    ),
                    @Parameter(
                            name = "cursor",
                            description = """
                                    커서 기반 페이징을 위한 커서 값

                                    **첫 요청:** cursor 없이 요청
                                    **다음 페이지:** 이전 응답의 nextCursor 값 사용
                                    """,
                            example = "eyJpZCI6MTIzLCJzb3J0VmFsdWUiOiIyMDI0LTEyLTI1VDEwOjAwOjAwIn0="
                    ),
                    @Parameter(
                            name = "size",
                            description = "페이지 크기 (1-50, 기본값: 10)",
                            example = "10",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(
                                    type = "integer",
                                    minimum = "1",
                                    maximum = "50"
                            )
                    )
            }
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = FreeboardCursorResponse.class),
                            examples = @ExampleObject(
                                    value = "{\"posts\":[{\"postId\":101,\"author\":{\"userId\":\"author101\",\"nickname\":\"작성자\",\"profileImageUrl\":\"https://cdn.example.com/profile.jpg\",\"userType\":\"INHABITANT\"},\"category\":\"restaurant\",\"title\":\"테스트 제목\",\"contentPreview\":\"테스트 내용 미리보기...\",\"thumbnailUrl\":null,\"imageCount\":1,\"likeCount\":5,\"commentCount\":2,\"viewCount\":80,\"isLiked\":false,\"createdAt\":\"2025-01-01T10:00:00\",\"updatedAt\":null}],\"hasNext\":true,\"nextCursor\":\"eyJpZCI6MTAxLCJzb3J0VmFsdWUiOiIyMDI1LTAxLTAxVDEwOjAwOjAwIn0=\",\"size\":1,\"totalCount\":20}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 파라미터",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "잘못된 카테고리", value = "{\"code\": \"INVALID_CATEGORY\", \"message\": \"유효하지 않은 카테고리입니다.\"}"),
                                    @ExampleObject(name = "잘못된 정렬 값", value = "{\"code\": \"INVALID_ENUM_VALUE\", \"message\": \"'INVALID_SORT'은(는) 허용되지 않는 값입니다. 사용 가능한 값: [LATEST, LIKE, COMMENT, VIEW]\"}"),
                                    @ExampleObject(name = "잘못된 커서", value = "{\"code\": \"INVALID_CURSOR\", \"message\": \"유효하지 않은 커서 값입니다.\"}")
                            }
                    )
            )
    })
    @GetMapping
    public ResponseEntity<FreeboardCursorResponse> getPostsByCursor(
            @RequestParam(value = "category", required = false) Category category,
            @RequestParam(defaultValue = "LATEST") FreeboardSortType sort,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") int size
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

        log.info("자유게시판 목록 조회 요청: category={}, sort={}, cursor={}, size={}, userId={}",
                category, sort, cursor != null ? "present" : "null", size, userId != null ? userId : "anonymous");

        try {
            FreeboardCursorResponse response = freeboardService.getPostsByCursor(category, sort, size, cursor, userId);

            log.debug("자유게시판 목록 조회 완료: resultCount={}, hasNext={}",
                    response.getPosts().size(), response.isHasNext());
            return ResponseEntity.ok(response);
        } catch (PostException e) {
            log.warn("자유게시판 목록 조회 중 비즈니스 예외: category={}, sort={}, cursor={}, size={}, userId={}, error={}",
                    category, sort, cursor, size, userId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("자유게시판 목록 조회 중 예기치 못한 예외: category={}, sort={}, cursor={}, size={}, userId={}",
                    category, sort, cursor, size, userId, e);
            throw e;
        }
    }

    @Operation(
            operationId = "updateFreeboardPost",
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
            @ApiResponse(
                    responseCode = "200",
                    description = "수정 성공",
                    content = @Content(schema = @Schema(implementation = FreeboardCreateResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (검증 실패, 이미지 관련 오류)",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "빈 제목", value = "{\"code\": \"VALIDATION_FAILED\", \"message\": \"[title] 제목은 필수입니다.\"}"),
                                    @ExampleObject(name = "빈 내용", value = "{\"code\": \"VALIDATION_FAILED\", \"message\": \"[content] 내용은 필수입니다.\"}"),
                                    @ExampleObject(name = "잘못된 카테고리", value = "{\"code\": \"INVALID_ENUM_VALUE\", \"message\": \"'invalid-category'은(는) 허용되지 않는 값입니다. 사용 가능한 값: [daily-hobby, restaurant, living-convenience, neighborhood-news, startup, others]\"}"),
                                    @ExampleObject(name = "이미지 개수 초과", value = "{\"code\": \"ILLEGAL_ARGUMENT\", \"message\": \"이미지는 최대 4장까지 업로드 가능합니다.\"}"),
                                    @ExampleObject(name = "지원하지 않는 파일 형식", value = "{\"code\": \"ILLEGAL_ARGUMENT\", \"message\": \"지원하지 않는 파일 형식입니다. 지원 형식: image/jpeg, image/jpg, image/png, image/gif, image/webp\"}"),
                                    @ExampleObject(name = "빈 파일", value = "{\"code\": \"ILLEGAL_ARGUMENT\", \"message\": \"이미지 파일이 비어있습니다.\"}")
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "413",
                    description = "파일 크기 초과 (Spring multipart 제한)",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"FILE_SIZE_EXCEEDED\", \"message\": \"파일 크기가 너무 큽니다. 최대 업로드 크기는 5MB입니다.\"}")
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
                            examples = @ExampleObject(value = "{\"code\": \"POST_ACCESS_DENIED\", \"message\": \"게시글에 대한 접근 권한이 없습니다.\"}")
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
    @PatchMapping(value = "/{freeboardId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Object> updatePost(
            @Parameter(description = "수정할 게시글 ID", example = "123")
            @PathVariable Long freeboardId,
            @ModelAttribute @Valid FreeboardUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            log.warn("자유게시판 글 수정 요청 시 인증 정보 없음: freeboardId={}", freeboardId);
            ErrorResponse errorResponse = new ErrorResponse("AUTHENTICATION_FAILED", "인증이 필요합니다.");
            return ResponseEntity.status(401).body(errorResponse);
        }

        log.info("자유게시판 글 수정 요청: freeboardId={}, userId={}",
                freeboardId, userDetails.getUser().getId());

        String userId = userDetails.getUser().getId();
        FreeboardCreateResponse response = freeboardService.updatePost(freeboardId, request, userId);

        log.info("자유게시판 글 수정 완료: freeboardId={}", response.getPostId());
        return ResponseEntity.ok(response);
    }

    @Operation(
            operationId = "deleteFreeboardPost",
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
                            examples = @ExampleObject(value = "{\"code\": \"POST_ACCESS_DENIED\", \"message\": \"게시글에 대한 접근 권한이 없습니다.\"}")
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
    @DeleteMapping("/{freeboardId}")
    public ResponseEntity<Object> deletePost(
            @Parameter(description = "삭제할 게시글 ID", example = "123")
            @PathVariable Long freeboardId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            log.warn("자유게시판 글 삭제 요청 시 인증 정보 없음: freeboardId={}", freeboardId);
            ErrorResponse errorResponse = new ErrorResponse("AUTHENTICATION_FAILED", "인증이 필요합니다.");
            return ResponseEntity.status(401).body(errorResponse);
        }

        log.info("자유게시판 글 삭제 요청: freeboardId={}, userId={}",
                freeboardId, userDetails.getUser().getId());

        String userId = userDetails.getUser().getId();
        freeboardService.deletePost(freeboardId, userId);

        log.info("자유게시판 글 삭제 완료: freeboardId={}", freeboardId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            operationId = "hardDeleteFreeboardPost",
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
                            examples = @ExampleObject(value = "{\"code\": \"POST_ACCESS_DENIED\", \"message\": \"게시글에 대한 접근 권한이 없습니다.\"}")
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
    @DeleteMapping("/{freeboardId}/force")
    public ResponseEntity<Object> hardDeletePost(
            @Parameter(description = "영구 삭제할 게시글 ID", example = "123")
            @PathVariable Long freeboardId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            log.warn("자유게시판 글 영구 삭제 요청 시 인증 정보 없음: freeboardId={}", freeboardId);
            ErrorResponse errorResponse = new ErrorResponse("AUTHENTICATION_FAILED", "인증이 필요합니다.");
            return ResponseEntity.status(401).body(errorResponse);
        }

        log.warn("자유게시판 글 영구 삭제 요청: freeboardId={}, userId={}",
                freeboardId, userDetails.getUser().getId());

        String userId = userDetails.getUser().getId();
        freeboardService.hardDeletePost(freeboardId, userId);

        log.warn("자유게시판 글 영구 삭제 완료: freeboardId={}", freeboardId);
        return ResponseEntity.noContent().build();
    }
}
