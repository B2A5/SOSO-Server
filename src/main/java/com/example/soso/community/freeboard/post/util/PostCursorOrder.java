package com.example.soso.community.freeboard.post.util;

import com.example.soso.community.freeboard.post.domain.dto.PostSortType;
import com.example.soso.community.freeboard.post.domain.entity.QPost;
import com.querydsl.core.types.OrderSpecifier;
import org.springframework.stereotype.Component;

@Component
public class PostCursorOrder {

    public OrderSpecifier<?>[] build(QPost post, PostSortType sort) {
        return switch (sort) {
            case LATEST -> new OrderSpecifier[]{
                    post.createdAt.desc(),
                    post.id.desc()
            };
            case LIKE -> new OrderSpecifier[]{
                    post.likeCount.desc(),
                    post.id.desc()
            };
            case COMMENT -> new OrderSpecifier[]{
                    post.commentCount.desc(),
                    post.id.desc()
            };
            case VIEW -> new OrderSpecifier[]{
                    post.viewCount.desc(),
                    post.id.desc()
            };
        };
    }

}
