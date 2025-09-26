package com.example.soso.community.common.post.service;

import com.example.soso.community.common.post.domain.dto.PostCreateRequest;
import com.example.soso.community.common.post.domain.dto.PostCreateResponse;
import com.example.soso.community.common.post.domain.dto.PostCursorResponse;
import com.example.soso.community.common.post.domain.dto.PostResponse;
import com.example.soso.community.common.post.domain.dto.PostSortType;
import com.example.soso.community.common.post.domain.dto.PostUpdateRequest;
import com.example.soso.community.common.post.domain.entity.Category;

public interface PostService {

    PostCreateResponse createPost(PostCreateRequest request, String user);

    PostResponse getPost(Long postId, String userId);

    PostCreateResponse updatePost(Long postId, PostUpdateRequest request, String user);

    PostCursorResponse getPostsByCursor(Category category, PostSortType sort, int size, String cursor, Long idAfter, String userId);

    void deletePost(Long postId,String user);

    void hardDeletePost(Long postId, String user);
}
