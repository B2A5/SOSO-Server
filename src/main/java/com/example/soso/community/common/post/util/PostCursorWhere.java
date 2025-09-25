package com.example.soso.community.common.post.util;

import com.example.soso.community.common.post.domain.dto.PostSortType;
import com.example.soso.community.common.post.domain.entity.QPost;
import com.querydsl.core.BooleanBuilder;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class PostCursorWhere {

    public BooleanBuilder build(QPost post, PostSortType sort, String cursor, Long idAfter) {
        BooleanBuilder condition = new BooleanBuilder();

        if (cursor == null || idAfter == null) return condition; // 커서나 idAfter가 없으면 조건 없음

        switch (sort) {
            case LATEST -> {
                LocalDateTime createdAt = LocalDateTime.parse(cursor);
                condition.and(
                        post.createdDate.lt(createdAt)
                                .or(post.createdDate.eq(createdAt).and(post.id.lt(idAfter)))
                );
            }
            case LIKE -> {
                int likeCount = Integer.parseInt(cursor);
                condition.and(
                        post.likeCount.lt(likeCount)
                                .or(post.likeCount.eq(likeCount).and(post.id.lt(idAfter)))
                );
            }
            case COMMENT -> {
                int commentCount = Integer.parseInt(cursor);
                condition.and(
                        post.commentCount.lt(commentCount)
                                .or(post.commentCount.eq(commentCount).and(post.id.lt(idAfter)))
                );
            }
        }

        return condition;
    }
}
