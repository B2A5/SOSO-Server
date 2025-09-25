package com.example.soso.community.common.post.controller;

import com.example.soso.community.common.post.domain.dto.PostCreateRequest;
import com.example.soso.community.common.post.domain.dto.PostCreateResponse;
import com.example.soso.community.common.post.domain.dto.PostCursorResponse;
import com.example.soso.community.common.post.domain.dto.PostResponse;
import com.example.soso.community.common.post.domain.dto.PostSortType;
import com.example.soso.community.common.post.domain.dto.PostUpdateRequest;
import com.example.soso.community.common.post.domain.entity.Category;
import com.example.soso.community.common.post.service.PostService;
import com.example.soso.security.domain.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Post", description = "게시글 관련 API")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;

    @Operation(
            summary = "게시글 작성",
            description = "게시글을 작성합니다. 이미지 업로드는 Multipart 방식으로 처리됩니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "게시글 생성 성공", content = @Content(schema = @Schema(implementation = Long.class))),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청 형식")
            }
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostCreateResponse> createPost(
            @ModelAttribute @Valid PostCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            log.warn("createPost 요청 시 인증 정보 없음");
            return ResponseEntity.status(401).build();
        }

        String userId = userDetails.getUser().getId();
        PostCreateResponse postId = postService.createPost(request, userId);
        return ResponseEntity.ok(postId);
    }

    @Operation(
            summary = "게시글 보기",
            description = "게시글을 확인 합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "게시글 ID 반환"),
                    @ApiResponse(responseCode = "403", description = "수정 권한 없음")
            }
    )
    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> getPostDetail(@PathVariable Long postId,
                                                      @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            log.warn("getPostDetail 요청 시 인증 정보 없음: postId={}", postId);
            return ResponseEntity.status(401).build();
        }

        String userId = userDetails.getUser().getId();
        PostResponse response = postService.getPost(postId, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/cursor")
    @Operation(summary = "커서 기반 게시글 목록 조회", description = "카테고리 및 정렬 기준에 따라 커서 기반으로 게시글 목록을 조회합니다.")
    public ResponseEntity<PostCursorResponse> getPostsByCursor(
            @Parameter(description = "카테고리 (예: DAILYANDHOBBY, FOOD, QUESTION, ETC)", example = "FOOD")
            @RequestParam(required = false) Category category,

            @Parameter(description = "정렬 기준 (LATEST: 최신순, LIKE: 좋아요순, COMMENT: 댓글순)", example = "LATEST")
            @RequestParam(defaultValue = "LATEST") PostSortType sort,

            @Parameter(
                    description = """
                커서 값 (정렬 기준에 따라 마지막으로 받은 게시글의 필드 값)
                - 최신순: 마지막 게시글의 createdAt (예: 2025-07-28T16:00:00)
                - 좋아요순: 마지막 게시글의 likeCount (예: 123)
                - 댓글순: 마지막 게시글의 commentCount (예: 15)
                """,
                    example = "2025-07-28T16:00:00"
            )
            @RequestParam(required = false) String cursor,

            @Parameter(
                    description = "보조 정렬 기준으로 사용되는 마지막 게시글의 ID\n정렬 필드 값이 동일한 게시글 간 순서를 판별하기 위해 사용됩니다.",
                    example = "57"
            )
            @RequestParam(required = false) Long idAfter,

            @Parameter(description = "한 페이지에 가져올 게시글 수", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        if (customUserDetails == null) {
            log.warn("getPostsByCursor 요청 시 인증 정보 없음");
            return ResponseEntity.status(401).build();
        }

        String userId = customUserDetails.getUser().getId();
        PostCursorResponse response = postService.getPostsByCursor(category, sort, size, cursor, idAfter, userId);
        return ResponseEntity.ok(response);
    }



    @Operation(
            summary = "게시글 수정",
            description = "게시글을 수정합니다. 이미지 포함 시 Multipart 형식으로 전송됩니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "수정된 게시글 ID 반환"),
                    @ApiResponse(responseCode = "403", description = "수정 권한 없음")
            }
    )
    @PatchMapping(value = "/{postId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostCreateResponse> updatePost(
            @PathVariable Long postId,
            @ModelAttribute @Valid PostUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            log.warn("updatePost 요청 시 인증 정보 없음: postId={}", postId);
            return ResponseEntity.status(401).build();
        }

        String userId = userDetails.getUser().getId();
        PostCreateResponse updatedId = postService.updatePost(postId, request, userId);
        return ResponseEntity.ok(updatedId);
    }

    @Operation(
            summary = "게시글 삭제 (Soft Delete)",
            description = "게시글을 소프트 삭제합니다.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "삭제 성공"),
                    @ApiResponse(responseCode = "403", description = "삭제 권한 없음")
            }
    )
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            log.warn("deletePost 요청 시 인증 정보 없음: postId={}", postId);
            return ResponseEntity.status(401).build();
        }

        String userId = userDetails.getUser().getId();
        postService.deletePost(postId, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "게시글 강제 삭제 (Hard Delete)",
            description = "게시글을 영구 삭제합니다. 관리자 또는 권한 있는 사용자만 호출할 수 있습니다.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "영구 삭제 성공"),
                    @ApiResponse(responseCode = "403", description = "삭제 권한 없음")
            }
    )
    @DeleteMapping("/{postId}/force")
    public ResponseEntity<Void> hardDeletePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            log.warn("hardDeletePost 요청 시 인증 정보 없음: postId={}", postId);
            return ResponseEntity.status(401).build();
        }

        String userId = userDetails.getUser().getId();
        postService.hardDeletePost(postId, userId);
        return ResponseEntity.noContent().build();
    }
}
