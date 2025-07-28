package com.example.soso.post.domain.dto;

public record CursorDto(
        String cursor, // createdAt or likeCount or commentCount
        Long idAfter
) {}
