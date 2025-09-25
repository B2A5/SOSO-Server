package com.example.soso.community.common.post.repository;

import com.example.soso.community.common.likes.domain.QPostLike;
import com.example.soso.community.common.post.domain.dto.PostSortType;
import com.example.soso.community.common.post.domain.dto.PostSummaryResponse;
import com.example.soso.community.common.post.domain.entity.Category;
import com.example.soso.community.common.post.domain.entity.Post;
import com.example.soso.community.common.post.domain.entity.QPost;
import com.example.soso.community.common.post.util.PostCursorOrder;
import com.example.soso.community.common.post.util.PostCursorWhere;
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
    // JPAQueryFactory는 Querydsl을 사용하여 데이터베이스 쿼리를 작성하고 실행하는 데 사용되는 클래스입니다.
    private final JPAQueryFactory queryFactory;
    // PostCursorWhere와 PostCursorOrder는 커서 기반 페이징을 위한 조건과 정렬을 처리하는 유틸리티 클래스입니다.
    private final PostCursorWhere postCursorWhere;
    // PostCursorOrder는 커서 기반 페이징을 위한 정렬 방식을 정의하는 유틸리티 클래스입니다.
    private final PostCursorOrder postCursorOrder;

    // findAllByCursorPaging 메서드는 카테고리와 정렬 기준에 따라 커서 기반으로 게시글 목록을 조회하는 기능을 제공합니다.
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
