package com.example.soso.post.service;

import com.example.soso.post.domain.dto.PostCreateRequest;
import com.example.soso.post.domain.dto.PostCreateResponse;
import com.example.soso.post.domain.dto.PostResponse;
import com.example.soso.post.domain.dto.PostUpdateRequest;

public interface PostService {

    PostCreateResponse createPost(PostCreateRequest request, String user);

    PostResponse getPost(Long postId, String userId);

    PostCreateResponse updatePost(Long postId, PostUpdateRequest request, String user);

    void deletePost(Long postId,String user);

    void hardDeletePost(Long postId, String user);
}
