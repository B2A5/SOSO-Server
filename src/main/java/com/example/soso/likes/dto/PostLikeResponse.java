package com.example.soso.likes.dto;

public record PostLikeResponse(
        boolean liked,
        long likeCount
) {}

