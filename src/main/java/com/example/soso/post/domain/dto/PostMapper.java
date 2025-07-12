package com.example.soso.post.domain.dto;

import com.example.soso.post.domain.entity.Post;
import com.example.soso.users.domain.entity.Users;

public class PostMapper {

    public static Post toEntity(PostCreateRequest dto, Users user) {
        return Post.builder()
                .title(dto.title())
                .content(dto.content())
                .category(dto.category())
                .imageUrls(dto.imageUrls())
                .user(user)
                .likeCount(0)
                .commentCount(0)
                .build();
    }

    public static PostResponse toResponse(Post post) {
        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getCategory(),
                post.getImageUrls(),
                post.getLikeCount(),
                post.getCommentCount(),
                post.getCreatedAt().toString(),
                toUserSummary(post.getUser())
        );
    }



    public static UserSummaryResponse toUserSummary(Users user) {
        return new UserSummaryResponse(
                user.getNickname(),
                user.getLocation(),
                user.getProfileImageUrl()
        );
    }
}
