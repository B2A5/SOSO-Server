package com.example.soso.community.common.comment.service;

import com.example.soso.community.common.comment.domain.dto.CommentCreateRequest;
import com.example.soso.community.common.comment.domain.dto.PostCommentResponse;
import java.util.List;


public interface CommentService {

    void create(Long postId, String userId, CommentCreateRequest request);

    void update(Long postId, Long commentId, String userId, CommentCreateRequest request);

    List<PostCommentResponse> getcomments(Long postId, String userId);

    void delete(Long postId, Long commentId, String userId);
}
