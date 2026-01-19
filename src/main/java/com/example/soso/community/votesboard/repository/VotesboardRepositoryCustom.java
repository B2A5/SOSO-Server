package com.example.soso.community.votesboard.repository;

import com.example.soso.community.votesboard.domain.entity.Votesboard;
import com.example.soso.community.votesboard.domain.entity.VoteStatus;
import com.example.soso.community.votesboard.dto.VoteboardSortType;

import java.util.List;

/**
 * 투표 게시글 커스텀 Repository 인터페이스
 */
public interface VotesboardRepositoryCustom {

    /**
     * 정렬 기준과 상태에 따라 커서 기반 투표 게시글 목록 조회
     *
     * @param status 투표 상태 (null이면 전체)
     * @param sort   정렬 기준 (LATEST, LIKE, COMMENT, VIEW)
     * @param cursor 커서 ID
     * @param size   조회 개수
     * @return 투표 게시글 목록
     */
    List<Votesboard> findAllBySortAndCursor(VoteStatus status, VoteboardSortType sort, Long cursor, int size);
}
