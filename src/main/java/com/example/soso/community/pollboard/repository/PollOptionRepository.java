package com.example.soso.community.pollboard.repository;

import com.example.soso.community.pollboard.domain.entity.PollOption;
import com.example.soso.community.pollboard.domain.entity.Poll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 투표 옵션 레포지토리
 */
@Repository
public interface PollOptionRepository extends JpaRepository<PollOption, Long> {

    /**
     * 특정 투표 게시글의 모든 옵션 조회 (순서대로)
     */
    @Query("SELECT po FROM PollOption po WHERE po.poll = :poll ORDER BY po.sequence ASC")
    List<PollOption> findByPollOrderBySequenceAsc(@Param("poll") Poll poll);

    /**
     * 특정 투표 게시글의 옵션 개수 조회
     */
    long countByPoll(Poll poll);

    /**
     * 특정 투표 게시글의 모든 옵션 삭제
     */
    void deleteByPoll(Poll poll);
}
