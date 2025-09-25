package com.example.soso.community.common.comment.domain.dto;

import com.example.soso.community.common.comment.domain.entity.Comment;
import com.example.soso.community.common.post.domain.entity.Post;
import com.example.soso.users.domain.dto.UserMapper;
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

    public static PostCommentResponse toResponse(Comment comment) {
        return new PostCommentResponse(
                comment.getId(),
                comment.getContent(),
                comment.getLikeCount(),
                comment.getLastModifiedDate().toString(),
                UserMapper.toUserSummary(comment.getUser())
        );
    }
    }
