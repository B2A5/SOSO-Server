package com.example.soso.post.domain.dto;

import com.example.soso.post.domain.entity.Category;
import java.util.List;

public record PostResponse(
        Long postId,
        String title,
        String content,
        Category category,
        List<String> imageUrls,
        int likeCount,
        int commentCount,
        String createdAt,
        UserSummaryResponse user
) {}
