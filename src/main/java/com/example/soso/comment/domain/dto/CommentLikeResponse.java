package com.example.soso.comment.domain.dto;

public record CommentLikeResponse(
        boolean liked,   // 좋아요 했는지 여부
        long likeCount   // 현재 좋아요 수
) {}
