package com.example.soso.community.votesboard.repository;

import com.example.soso.community.votesboard.domain.entity.QVotesboard;
import com.example.soso.community.votesboard.domain.entity.Votesboard;
import com.example.soso.community.votesboard.domain.entity.VoteStatus;
import com.example.soso.community.votesboard.dto.VoteboardSortType;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 투표 게시글 커스텀 Repository 구현체
 */
@Repository
@RequiredArgsConstructor
public class VotesboardRepositoryImpl implements VotesboardRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Votesboard> findAllBySortAndCursor(VoteStatus status, VoteboardSortType sort, Long cursor, int size) {
        QVotesboard votesboard = QVotesboard.votesboard;
        LocalDateTime now = LocalDateTime.now();

        BooleanBuilder condition = new BooleanBuilder();
        // 삭제되지 않은 게시글만 조회
        condition.and(votesboard.deleted.eq(false));

        // 상태 필터링
        if (status == VoteStatus.IN_PROGRESS) {
            condition.and(votesboard.endTime.gt(now));
        } else if (status == VoteStatus.COMPLETED) {
            condition.and(votesboard.endTime.loe(now));
        }

        // 커서 조건 추가
        if (cursor != null) {
            condition.and(getCursorCondition(votesboard, sort, cursor));
        }

        // 정렬 설정
        OrderSpecifier<?>[] orderSpecifiers = getOrderSpecifiers(votesboard, sort);

        return queryFactory
                .selectFrom(votesboard)
                .where(condition)
                .orderBy(orderSpecifiers)
                .limit(size + 1)
                .fetch();
    }

    /**
     * 정렬 기준에 따른 커서 조건 생성
     */
    private BooleanExpression getCursorCondition(QVotesboard votesboard, VoteboardSortType sort, Long cursorId) {
        // 커서 ID에 해당하는 게시글의 정렬 기준 값을 가져와서 비교
        // 간단하게 ID만으로 커서 처리 (복잡한 정렬의 경우 서브쿼리 필요)
        return votesboard.id.lt(cursorId);
    }

    /**
     * 정렬 기준에 따른 OrderSpecifier 생성
     */
    private OrderSpecifier<?>[] getOrderSpecifiers(QVotesboard votesboard, VoteboardSortType sort) {
        switch (sort) {
            case LIKE:
                // 투표 인원 순 (totalVotes 내림차순), 동점 시 최신순
                return new OrderSpecifier<?>[]{
                        votesboard.totalVotes.desc(),
                        votesboard.id.desc()
                };
            case COMMENT:
                // 댓글 순 (commentCount 내림차순), 동점 시 최신순
                return new OrderSpecifier<?>[]{
                        votesboard.commentCount.desc(),
                        votesboard.id.desc()
                };
            case VIEW:
                // 조회 순 (viewCount 내림차순), 동점 시 최신순
                return new OrderSpecifier<?>[]{
                        votesboard.viewCount.desc(),
                        votesboard.id.desc()
                };
            case LATEST:
            default:
                // 최신순 (ID 내림차순)
                return new OrderSpecifier<?>[]{
                        votesboard.id.desc()
                };
        }
    }
}
