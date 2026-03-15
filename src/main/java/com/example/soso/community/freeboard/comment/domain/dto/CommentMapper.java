package com.example.soso.community.freeboard.comment.domain.dto;

import com.example.soso.community.freeboard.comment.domain.entity.PostComment;
import com.example.soso.community.freeboard.post.domain.entity.Post;
import com.example.soso.users.domain.dto.UserMapper;
import com.example.soso.users.domain.entity.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentMapper {

    private final UserMapper userMapper;

    public PostComment toEntity(CommentCreateRequest dto, Post post, Users user) {
        return PostComment.builder()
                .post(post)
                .user(user)
                .content(dto.content())
                .build();
    }

    public PostCommentResponse toResponse(PostComment comment) {
        return new PostCommentResponse(
                comment.getId(),
                comment.getContent(),
                comment.getLikeCount(),
                comment.getUpdatedAt().toString(),
                userMapper.toUserSummary(comment.getUser())
        );
    }
}
