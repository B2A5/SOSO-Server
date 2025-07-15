package com.example.soso.comment.service;

import com.example.soso.comment.domain.dto.CommentCreateRequest;
import com.example.soso.comment.domain.dto.CommentResponse;


public interface CommentService {

    CommentResponse create(Long postId, String userId, CommentCreateRequest request);

    CommentResponse update(Long commentId, String userId, CommentCreateRequest request);

    void delete(Long commentId, String userId);
}
