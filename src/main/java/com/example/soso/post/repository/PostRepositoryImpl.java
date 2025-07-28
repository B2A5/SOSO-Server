package com.example.soso.post.repository;

import com.example.soso.likes.domain.QPostLike;
import com.example.soso.post.domain.dto.PostSortType;
import com.example.soso.post.domain.dto.PostSummaryResponse;
import com.example.soso.post.domain.entity.Category;
import com.example.soso.post.domain.entity.Post;
import com.example.soso.post.domain.entity.QPost;
import com.example.soso.post.util.PostCursorOrder;
import com.example.soso.post.util.PostCursorWhere;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PostRepositoryImpl implements PostRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final PostCursorWhere postCursorWhere;
    private final PostCursorOrder postCursorOrder;

    public List<PostSummaryResponse> findAllByCursorPaging(
            Category category,
            PostSortType sort,
            int size,
            String cursor,
            Long idAfter,
            String userId
    ) {
        QPost post = QPost.post;
        QPostLike like = QPostLike.postLike;

        BooleanBuilder condition = new BooleanBuilder();
        if (category != null) {
            condition.and(post.category.eq(category));
        }
        condition.and(postCursorWhere.build(post, sort, cursor, idAfter));
        OrderSpecifier<?>[] order = postCursorOrder.build(post, sort);

        return queryFactory
                .select(Projections.constructor(PostSummaryResponse.class,
                        post.id,
                        post.title,
                        post.content,
                        post.likeCount,
                        post.commentCount,
                        like.id.isNotNull() // 내가 좋아요 눌렀는지 여부
                ))
                .from(post)
                .leftJoin(like)
                .on(like.post.id.eq(post.id)
                        .and(like.user.id.eq(userId)))
                .where(condition)
                .orderBy(order)
                .limit(size + 1)
                .fetch();
    }

}
