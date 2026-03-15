package com.example.soso.community.freeboard.post.domain.dto;

import com.example.soso.community.common.post.domain.dto.UserSummaryResponse;
import com.example.soso.community.freeboard.post.domain.entity.Post;
import com.example.soso.community.freeboard.post.domain.entity.PostImage;
import com.example.soso.users.domain.dto.UserMapper;
import com.example.soso.users.domain.entity.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostMapper {

    private final UserMapper userMapper;

    public Post toEntity(PostCreateRequest dto, Users user) {
        return Post.create(user, dto.title(), dto.content(), dto.category());
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
                post.getCreatedAt().toString(),
                userMapper.toUserSummary(post.getUser())
        );
    }
}
