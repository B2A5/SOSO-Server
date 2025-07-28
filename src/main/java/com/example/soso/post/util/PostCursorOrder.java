package com.example.soso.post.util;

import com.example.soso.post.domain.dto.PostSortType;
import com.example.soso.post.domain.entity.QPost;
import com.querydsl.core.types.OrderSpecifier;
import org.springframework.stereotype.Component;

@Component
public class PostCursorOrder {

    public OrderSpecifier<?>[] build(QPost post, PostSortType sort) {
        return switch (sort) {
            case LATEST -> new OrderSpecifier[]{
                    post.createdDate.desc(),
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
        };
    }

}
