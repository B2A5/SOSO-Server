package com.example.soso.community.common.comment.domain.dto;

import com.example.soso.community.common.comment.domain.entity.Comment;
import com.example.soso.community.common.post.domain.entity.Post;
import com.example.soso.users.domain.dto.UserMapper;
import com.example.soso.users.domain.entity.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentMapper {

    private final UserMapper userMapper;

    public Comment toEntity(CommentCreateRequest dto, Post post, Users user) {
        return Comment.builder()
                .post(post)
                .user(user)
                .content(dto.content())
                .likeCount(0)
                .build();
    }

    public PostCommentResponse toResponse(Comment comment) {
        return new PostCommentResponse(
                comment.getId(),
                comment.getContent(),
                comment.getLikeCount(),
                comment.getUpdatedAt().toString(),
                userMapper.toUserSummary(comment.getUser())
        );
    }
}
