package com.example.soso.community.pollboard.repository;

import com.example.soso.community.pollboard.domain.entity.Poll;
import com.example.soso.community.pollboard.domain.entity.PollStatus;
import com.example.soso.community.pollboard.dto.PollSortType;

import java.util.List;

/**
 * 투표 게시글 커스텀 Repository 인터페이스
 */
public interface PollRepositoryCustom {

    /**
     * 정렬 기준과 상태에 따라 커서 기반 투표 게시글 목록 조회
     *
     * @param status 투표 상태 (null이면 전체)
     * @param sort   정렬 기준 (LATEST, LIKE, COMMENT, VIEW)
     * @param cursor 커서 ID
     * @param size   조회 개수
     * @return 투표 게시글 목록
     */
    List<Poll> findAllBySortAndCursor(PollStatus status, PollSortType sort, Long cursor, int size);
}
