package com.example.soso.post.service;

import com.example.soso.post.domain.dto.PostCreateRequest;
import com.example.soso.post.domain.dto.PostResponse;
import com.example.soso.post.domain.dto.PostUpdateRequest;
import com.example.soso.users.domain.entity.Users;

public interface PostService {

    Long createPost(PostCreateRequest request, Users user);

    PostResponse getPost(Long postId);

    Long updatePost(Long postId, PostUpdateRequest request, Users user);

    void deletePost(Long postId, Users user);

    void hardDeletePost(Long postId, Users user);
}
