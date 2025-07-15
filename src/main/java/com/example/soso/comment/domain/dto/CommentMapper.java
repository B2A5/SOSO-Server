package com.example.soso.comment.domain.dto;

import com.example.soso.comment.domain.entity.Comment;
import com.example.soso.post.domain.dto.PostMapper;
import com.example.soso.post.domain.entity.Post;
import com.example.soso.users.domain.entity.Users;

public class CommentMapper {

    public static Comment toEntity(CommentCreateRequest dto, Post post, Users user) {
        return Comment.builder()
                .post(post)
                .user(user)
                .content(dto.content())
                .likeCount(0)
                .build();
    }

    public static CommentResponse toResponse(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                comment.getLikeCount(),
                comment.getLastModifiedDate().toString(),

                PostMapper.toUserSummary(comment.getUser())
        );
    }
}
