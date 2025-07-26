package com.example.soso.comment.controller;

import com.example.soso.comment.domain.dto.CommentLikeResponse;
import com.example.soso.comment.service.CommentLikeService;
import com.example.soso.security.domain.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Comment Like", description = "댓글 좋아요 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/comments")
public class CommentLikeController {

    private final CommentLikeService commentLikeService;

    @Operation(summary = "댓글 좋아요", description = "댓글에 좋아요를 등록합니다.")
    @PostMapping("/{commentId}/like")
    public ResponseEntity<CommentLikeResponse> likeComment(@PathVariable Long commentId,
                                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        String userId = userDetails.getUser().getId();
        CommentLikeResponse response = commentLikeService.likeComment(commentId, userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "댓글 좋아요 취소", description = "댓글에 눌렀던 좋아요를 취소합니다.")
    @DeleteMapping("/{commentId}/like")
    public ResponseEntity<CommentLikeResponse> unlikeComment(@PathVariable Long commentId,
                                                             @AuthenticationPrincipal CustomUserDetails userDetails) {
        CommentLikeResponse result = commentLikeService.unlikeComment(commentId, userDetails.getUser().getId());
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "댓글 좋아요 여부 조회", description = "현재 사용자가 댓글에 좋아요를 눌렀는지 확인합니다.")
    @GetMapping("/{commentId}/like")
    public ResponseEntity<Boolean> isLiked(@PathVariable Long commentId,
                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        boolean liked = commentLikeService.isLiked(commentId, userDetails.getUser().getId());
        return ResponseEntity.ok(liked);
    }

    @Operation(summary = "댓글 좋아요 수 조회", description = "해당 댓글의 좋아요 수를 반환합니다.")
    @GetMapping("/{commentId}/like-count")
    public ResponseEntity<Long> getLikeCount(@PathVariable Long commentId) {
        long count = commentLikeService.getLikeCount(commentId);
        return ResponseEntity.ok(count);
    }
}
