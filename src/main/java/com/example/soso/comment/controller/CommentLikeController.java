package com.example.soso.comment.controller;

import com.example.soso.comment.domain.dto.CommentLikeResponse;
import com.example.soso.comment.service.CommentLikeService;
import com.example.soso.security.domain.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/comments")
public class CommentLikeController {

    private final CommentLikeService commentLikeService;

    @PostMapping("/{commentId}/like")
    public ResponseEntity<CommentLikeResponse> likeComment(@PathVariable Long commentId,
                                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        String userId = userDetails.getUser().getId();
        CommentLikeResponse response = commentLikeService.likeComment(commentId, userId);
        return ResponseEntity.ok(response);
    }


    @DeleteMapping("/{commentId}/like")
    public ResponseEntity<CommentLikeResponse> unlikeComment(@PathVariable Long commentId,
                                                 @AuthenticationPrincipal CustomUserDetails userDetails) {
        CommentLikeResponse result = commentLikeService.unlikeComment(commentId, userDetails.getUser().getId());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{commentId}/like")
    public ResponseEntity<Boolean> isLiked(@PathVariable Long commentId,
                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        boolean liked = commentLikeService.isLiked(commentId, userDetails.getUser().getId());
        return ResponseEntity.ok(liked);
    }

    @GetMapping("/{commentId}/like-count")
    public ResponseEntity<Long> getLikeCount(@PathVariable Long commentId) {
        long count = commentLikeService.getLikeCount(commentId);
        return ResponseEntity.ok(count);
    }
}
