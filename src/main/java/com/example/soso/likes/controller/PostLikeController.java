package com.example.soso.likes.controller;

import com.example.soso.likes.dto.PostLikeResponse;
import com.example.soso.likes.service.PostLikeService;
import com.example.soso.security.domain.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Post Like", description = "게시글 좋아요 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/posts")
public class PostLikeController {

    private final PostLikeService postLikeService;

    @Operation(summary = "게시글 좋아요", description = "게시글에 좋아요를 누릅니다.")
    @ApiResponse(responseCode = "200", description = "좋아요 처리 성공",
            content = @Content(schema = @Schema(implementation = PostLikeResponse.class)))
    @PostMapping("/{postId}/like")
    public ResponseEntity<PostLikeResponse> likePost(@PathVariable Long postId,
                                                     @AuthenticationPrincipal CustomUserDetails userDetails) {
        PostLikeResponse response = postLikeService.likePost(postId, userDetails.getUser().getId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "게시글 좋아요 취소", description = "게시글에 누른 좋아요를 취소합니다.")
    @ApiResponse(responseCode = "200", description = "좋아요 취소 성공",
            content = @Content(schema = @Schema(implementation = PostLikeResponse.class)))
    @DeleteMapping("/{postId}/like")
    public ResponseEntity<PostLikeResponse> unlikePost(@PathVariable Long postId,
                                                       @AuthenticationPrincipal CustomUserDetails userDetails) {
        PostLikeResponse response = postLikeService.unlikePost(postId, userDetails.getUser().getId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "게시글 좋아요 여부 확인", description = "사용자가 해당 게시글에 좋아요를 눌렀는지 확인합니다.")
    @ApiResponse(responseCode = "200", description = "좋아요 여부 반환", content = @Content(schema = @Schema(implementation = Boolean.class)))
    @GetMapping("/{postId}/like")
    public ResponseEntity<Boolean> isLiked(@PathVariable Long postId,
                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        boolean liked = postLikeService.isPostLiked(postId, userDetails.getUser().getId());
        return ResponseEntity.ok(liked);
    }

    @Operation(summary = "게시글 좋아요 수 조회", description = "해당 게시글의 총 좋아요 수를 반환합니다.")
    @ApiResponse(responseCode = "200", description = "좋아요 수 반환", content = @Content(schema = @Schema(implementation = Long.class)))
    @GetMapping("/{postId}/like/count")
    public ResponseEntity<Long> getLikeCount(@PathVariable Long postId) {
        long count = postLikeService.getPostLikeCount(postId);
        return ResponseEntity.ok(count);
    }
}
