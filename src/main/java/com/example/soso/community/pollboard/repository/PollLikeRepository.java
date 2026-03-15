package com.example.soso.community.pollboard.repository;

import com.example.soso.community.pollboard.domain.entity.Poll;
import com.example.soso.community.pollboard.domain.entity.PollLike;
import com.example.soso.users.domain.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 투표 게시글 좋아요 레포지토리
 */
@Repository
public interface PollLikeRepository extends JpaRepository<PollLike, Long> {

    /**
     * 특정 투표 게시글에 대한 사용자의 좋아요 조회
     */
    Optional<PollLike> findByPollAndUser(Poll poll, Users user);

    /**
     * 특정 투표 게시글에 대한 사용자의 좋아요 존재 여부 확인
     */
    boolean existsByPollAndUser(Poll poll, Users user);

    /**
     * 특정 투표 게시글의 좋아요 개수 조회
     */
    long countByPoll(Poll poll);

    /**
     * 특정 투표 게시글 ID에 대한 사용자의 좋아요 존재 여부 확인
     */
    @Query("SELECT CASE WHEN COUNT(pl) > 0 THEN true ELSE false END " +
           "FROM PollLike pl " +
           "WHERE pl.poll.id = :pollId AND pl.user.id = :userId")
    boolean existsByPollIdAndUserId(@Param("pollId") Long pollId, @Param("userId") String userId);

    /**
     * 특정 투표 게시글 ID의 좋아요 개수 조회
     */
    @Query("SELECT COUNT(pl) FROM PollLike pl WHERE pl.poll.id = :pollId")
    long countByPollId(@Param("pollId") Long pollId);
}
