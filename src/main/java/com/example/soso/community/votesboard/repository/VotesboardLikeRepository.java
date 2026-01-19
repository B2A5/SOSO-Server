package com.example.soso.community.votesboard.repository;

import com.example.soso.community.votesboard.domain.entity.Votesboard;
import com.example.soso.community.votesboard.domain.entity.VotesboardLike;
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
public interface VotesboardLikeRepository extends JpaRepository<VotesboardLike, Long> {

    /**
     * 특정 투표 게시글에 대한 사용자의 좋아요 조회
     */
    Optional<VotesboardLike> findByVotesboardAndUser(Votesboard votesboard, Users user);

    /**
     * 특정 투표 게시글에 대한 사용자의 좋아요 존재 여부 확인
     */
    boolean existsByVotesboardAndUser(Votesboard votesboard, Users user);

    /**
     * 특정 투표 게시글의 좋아요 개수 조회
     */
    long countByVotesboard(Votesboard votesboard);

    /**
     * 특정 투표 게시글 ID에 대한 사용자의 좋아요 존재 여부 확인
     */
    @Query("SELECT CASE WHEN COUNT(vl) > 0 THEN true ELSE false END " +
           "FROM VotesboardLike vl " +
           "WHERE vl.votesboard.id = :votesboardId AND vl.user.id = :userId")
    boolean existsByVotesboardIdAndUserId(@Param("votesboardId") Long votesboardId, @Param("userId") String userId);

    /**
     * 특정 투표 게시글 ID의 좋아요 개수 조회
     */
    @Query("SELECT COUNT(vl) FROM VotesboardLike vl WHERE vl.votesboard.id = :votesboardId")
    long countByVotesboardId(@Param("votesboardId") Long votesboardId);
}
