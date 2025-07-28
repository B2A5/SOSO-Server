package com.example.soso.post.domain.dto;

public record PostSummaryResponse(
        Long postId,
        String title,
        String content,
        String category,
        int likeCount,
        int commentCount,
        String createdAt,
        UserSummaryResponse user
) {}
