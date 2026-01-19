package com.example.soso.community.voteboard.repository;

import com.example.soso.community.voteboard.domain.entity.Votesboard;
import com.example.soso.community.voteboard.domain.entity.VoteResult;
import com.example.soso.users.domain.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 투표 결과 레포지토리
 */
@Repository
public interface VoteResultRepository extends JpaRepository<VoteResult, Long> {

    /**
     * 특정 사용자의 특정 투표에 대한 투표 결과 조회 (단일 선택용)
     */
    Optional<VoteResult> findByUserAndVotesboard(Users user, Votesboard votesboard);

    /**
     * 특정 사용자의 특정 투표에 대한 모든 투표 결과 조회 (중복 선택용)
     */
    List<VoteResult> findAllByUserAndVotesboard(Users user, Votesboard votesboard);

    /**
     * 특정 사용자가 특정 투표에 참여했는지 확인
     */
    boolean existsByUserAndVotesboard(Users user, Votesboard votesboard);

    /**
     * 특정 투표의 모든 투표 결과 조회
     */
    List<VoteResult> findByVotesboard(Votesboard votesboard);

    /**
     * 특정 투표의 전체 투표 수 조회
     */
    long countByVotesboard(Votesboard votesboard);

    /**
     * 특정 사용자가 참여한 모든 투표 조회
     */
    List<VoteResult> findByUser(Users user);

    /**
     * 특정 투표의 모든 투표 결과 삭제
     */
    void deleteByVotesboard(Votesboard votesboard);
}
