package com.example.soso.comment.domain.dto;

import com.example.soso.post.domain.dto.UserSummaryResponse;

public record CommentResponse(
        Long id,
        String content,
        int likeCount,
        String createdAt,
        UserSummaryResponse user
) {}