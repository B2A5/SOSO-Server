package com.example.soso.comment.service;

import com.example.soso.comment.domain.dto.CommentCreateRequest;
import com.example.soso.comment.domain.dto.PostCommentResponse;
import java.util.List;


public interface CommentService {

    void create(Long postId, String userId, CommentCreateRequest request);

    void update(Long commentId, String userId, CommentCreateRequest request);

    List<PostCommentResponse> getcomments(Long postId, String userId);

    void delete(Long commentId, String userId);
}
