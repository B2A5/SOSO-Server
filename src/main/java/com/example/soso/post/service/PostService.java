package com.example.soso.post.service;

import com.example.soso.post.domain.dto.PostCreateRequest;
import com.example.soso.post.domain.dto.PostResponse;
import com.example.soso.users.domain.entity.Users;


public interface PostService {

    Long createPost(PostCreateRequest request, Users user);

    PostResponse getPost(Long postId);
}
