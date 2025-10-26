package com.example.soso.community.common.post.domain.dto;

import com.example.soso.community.common.post.domain.entity.Post;
import com.example.soso.community.common.post.domain.entity.PostImage;
import com.example.soso.users.domain.dto.UserMapper;
import com.example.soso.users.domain.entity.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostMapper {

    private final UserMapper userMapper;

    public Post toEntity(PostCreateRequest dto, Users user) {
        return Post.builder()
                .title(dto.title())
                .content(dto.content())
                .category(dto.category())
                .user(user)
                .likeCount(0)
                .commentCount(0)
                .build();
    }

    public PostResponse toResponse(Post post, boolean isLiked) {
        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getCategory(),
                post.getImages().stream()
                        .map(PostImage::getImageUrl)
                        .toList(),
                post.getLikeCount(),
                isLiked,
                post.getCreatedDate().toString(),
                userMapper.toUserSummary(post.getUser())
        );
    }
}
