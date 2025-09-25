package com.example.soso.community.freeboard.like.controller;

import com.example.soso.community.freeboard.like.service.FreeboardLikeService;
import com.example.soso.security.domain.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 자유게시판 좋아요 API 컨트롤러
 */
@Tag(name = "자유게시판 좋아요", description = "자유게시판 게시글 좋아요 관련 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/community/freeboard/{postId}/like")
@RequiredArgsConstructor
public class FreeboardLikeController {

    private final FreeboardLikeService freeboardLikeService;

    @Operation(
        summary = "게시글 좋아요 토글",
        description = "게시글에 좋아요를 추가하거나 취소합니다. 이미 좋아요한 게시글이면 취소하고, 그렇지 않으면 추가합니다."
    )
    @PostMapping
    public ResponseEntity<Boolean> toggleLike(
        @Parameter(description = "게시글 ID", required = true)
        @PathVariable Long postId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            log.warn("toggleLike 요청 시 인증 정보 없음: postId={}", postId);
            return ResponseEntity.status(401).build();
        }

        log.info("자유게시판 게시글 좋아요 토글: postId={}, userId={}", postId, userDetails.getUsername());

        boolean isLiked = freeboardLikeService.toggleLike(postId, userDetails.getUsername());

        return ResponseEntity.ok(isLiked);
    }

    @Operation(
        summary = "게시글 좋아요 상태 확인",
        description = "사용자가 특정 게시글에 좋아요를 했는지 확인합니다."
    )
    @GetMapping
    public ResponseEntity<Boolean> getLikeStatus(
        @Parameter(description = "게시글 ID", required = true)
        @PathVariable Long postId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            log.warn("getLikeStatus 요청 시 인증 정보 없음: postId={}", postId);
            return ResponseEntity.status(401).build();
        }

        log.debug("자유게시판 게시글 좋아요 상태 조회: postId={}, userId={}", postId, userDetails.getUsername());

        boolean isLiked = freeboardLikeService.isLikedByUser(postId, userDetails.getUsername());

        return ResponseEntity.ok(isLiked);
    }
}