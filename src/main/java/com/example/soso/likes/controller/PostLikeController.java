package com.example.soso.likes.controller;

import com.example.soso.likes.dto.PostLikeResponse;
import com.example.soso.likes.service.PostLikeService;
import com.example.soso.security.domain.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/posts")
public class PostLikeController {

    private final PostLikeService postLikeService;

    @PostMapping("/{postId}/like")
    public ResponseEntity<PostLikeResponse> likePost(@PathVariable Long postId,
                                                     @AuthenticationPrincipal CustomUserDetails userDetails) {
        PostLikeResponse response = postLikeService.likePost(postId, userDetails.getUser().getId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{postId}/like")
    public ResponseEntity<PostLikeResponse> unlikePost(@PathVariable Long postId,
                                                       @AuthenticationPrincipal CustomUserDetails userDetails) {
        PostLikeResponse response = postLikeService.unlikePost(postId, userDetails.getUser().getId());
        return ResponseEntity.ok(response);
    }


    @GetMapping("/{postId}/like")
    public ResponseEntity<Boolean> isLiked(@PathVariable Long postId,
                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        boolean liked = postLikeService.isPostLiked(postId, userDetails.getUser().getId());
        return ResponseEntity.ok(liked);
    }

    @GetMapping("/{postId}/like/count")
    public ResponseEntity<Long> getLikeCount(@PathVariable Long postId) {
        long count = postLikeService.getPostLikeCount(postId);
        return ResponseEntity.ok(count);
    }
}
