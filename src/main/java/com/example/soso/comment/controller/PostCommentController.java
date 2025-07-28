package com.example.soso.comment.controller;

import com.example.soso.comment.domain.dto.CommentCreateRequest;
import com.example.soso.comment.domain.dto.PostCommentResponse;
import com.example.soso.comment.service.CommentService;
import com.example.soso.security.domain.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/posts/{postId}/comments")
@RequiredArgsConstructor
public class PostCommentController {

    private final CommentService commentService;

    @GetMapping
    @Operation(summary = "게시글 댓글 목록 조회", description = "게시글에 달린 댓글을 조회합니다.")
    public ResponseEntity<List<PostCommentResponse>> getAllComments(@PathVariable Long postId,
                                                                    @AuthenticationPrincipal CustomUserDetails userDetails) {
        String userId = userDetails.getUser().getId();
        List<PostCommentResponse> response = commentService.getcomments(postId, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "댓글 작성", description = "게시글에 댓글을 작성합니다.")
    public ResponseEntity<Void> createComment(@PathVariable Long postId,
                                              @RequestBody CommentCreateRequest request,
                                              @AuthenticationPrincipal CustomUserDetails user) {
        commentService.create(postId, user.getUser().getId(), request);
        return ResponseEntity.status(201).build();
    }

    @PutMapping("/{commentId}")
    @Operation(summary = "댓글 수정", description = "댓글을 수정합니다.")
    public ResponseEntity<Void> updateComment(@PathVariable Long postId,
                                              @PathVariable Long commentId,
                                              @RequestBody CommentCreateRequest request,
                                              @AuthenticationPrincipal CustomUserDetails user) {
        commentService.update(postId, commentId, user.getUser().getId(), request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "댓글 삭제", description = "댓글을 삭제합니다.")
    public ResponseEntity<Void> deleteComment(@PathVariable Long postId,
                                              @PathVariable Long commentId,
                                              @AuthenticationPrincipal CustomUserDetails user) {
        commentService.delete(postId, commentId, user.getUser().getId());
        return ResponseEntity.noContent().build();
    }
}
