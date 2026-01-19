package com.example.soso.community.votesboard.repository;

import com.example.soso.community.votesboard.domain.entity.VoteOption;
import com.example.soso.community.votesboard.domain.entity.Votesboard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 투표 옵션 레포지토리
 */
@Repository
public interface VoteOptionRepository extends JpaRepository<VoteOption, Long> {

    /**
     * 특정 투표 게시글의 모든 옵션 조회 (순서대로)
     */
    @Query("SELECT vo FROM VoteOption vo WHERE vo.votesboard = :votesboard ORDER BY vo.sequence ASC")
    List<VoteOption> findByVotesboardOrderBySequenceAsc(@Param("votesboard") Votesboard votesboard);

    /**
     * 특정 투표 게시글의 옵션 개수 조회
     */
    long countByVotesboard(Votesboard votesboard);

    /**
     * 특정 투표 게시글의 모든 옵션 삭제
     */
    void deleteByVotesboard(Votesboard votesboard);
}
