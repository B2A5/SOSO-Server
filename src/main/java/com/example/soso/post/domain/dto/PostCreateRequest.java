package com.example.soso.post.domain.dto;

import com.example.soso.post.domain.entity.Category;
import java.util.List;

public record PostCreateRequest(
        String title,
        String content,
        Category category,
        List<String> imageUrls
) {}
