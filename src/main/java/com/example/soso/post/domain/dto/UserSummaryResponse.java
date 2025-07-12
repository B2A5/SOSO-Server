package com.example.soso.post.domain.dto;

public record UserSummaryResponse(
        String nickname,
        String location,
        String profileImageUrl
) {}

