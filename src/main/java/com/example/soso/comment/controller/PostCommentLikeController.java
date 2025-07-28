package com.example.soso.comment.controller;

import com.example.soso.comment.domain.dto.CommentLikeResponse;
import com.example.soso.comment.domain.dto.LikedCommentIdListResponse;
import com.example.soso.comment.service.PostCommentLikeService;
import com.example.soso.security.domain.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/posts/{postId}/comments")
@Tag(name = "Comment Like", description = "댓글 좋아요 관련 API")
public class PostCommentLikeController {

    private final PostCommentLikeService postCommentLikeService;

    @PostMapping("/{commentId}/like")
    @Operation(summary = "댓글 좋아요", description = "댓글에 좋아요를 등록합니다.")
    public ResponseEntity<CommentLikeResponse> likeComment(@PathVariable Long postId,
                                                           @PathVariable Long commentId,
                                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        String userId = userDetails.getUser().getId();
        CommentLikeResponse response = postCommentLikeService.likeComment(postId,commentId, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{commentId}/like")
    @Operation(summary = "댓글 좋아요 취소", description = "댓글에 눌렀던 좋아요를 취소합니다.")
    public ResponseEntity<CommentLikeResponse> unlikeComment(@PathVariable Long postId,
                                                             @PathVariable Long commentId,
                                                             @AuthenticationPrincipal CustomUserDetails userDetails) {
        String userId = userDetails.getUser().getId();
        CommentLikeResponse result = postCommentLikeService.unlikeComment(postId, commentId, userId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{commentId}/like")
    @Operation(summary = "댓글 좋아요 여부 조회", description = "현재 사용자가 댓글에 좋아요를 눌렀는지 확인합니다.")
    public ResponseEntity<Boolean> isLiked(@PathVariable Long postId,
                                           @PathVariable Long commentId,
                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        String userId = userDetails.getUser().getId();
        boolean liked = postCommentLikeService.isLiked(postId, commentId, userId);
        return ResponseEntity.ok(liked);
    }

    @GetMapping("/liked")
    @Operation(summary = "사용자가 좋아요 누른 댓글 ID 목록 조회", description = "해당 게시글 내에서 사용자가 좋아요를 누른 댓글들의 ID 목록을 조회합니다.")
    public ResponseEntity<LikedCommentIdListResponse> getLikedCommentIds(
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String userId = userDetails.getUser().getId();
        List<Long> likedIds = postCommentLikeService.getLikedCommentIds(postId, userId);
        return ResponseEntity.ok(new LikedCommentIdListResponse(likedIds));
    }
}
