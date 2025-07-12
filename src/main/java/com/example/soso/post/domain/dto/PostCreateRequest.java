package com.example.soso.post.domain.dto;

import com.example.soso.post.domain.entity.Category;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public record PostCreateRequest(
        String title,
        String content,
        Category category,
        List<MultipartFile> images
) {}
