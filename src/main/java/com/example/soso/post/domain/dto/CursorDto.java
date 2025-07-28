package com.example.soso.post.domain.dto;

public record CursorDto(
        Boolean hasNext,
        String cursor, // createdAt or likeCount or commentCount
        Long idAfter
) {}
