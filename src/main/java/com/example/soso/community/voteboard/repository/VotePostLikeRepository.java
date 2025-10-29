package com.example.soso.community.voteboard.repository;

import com.example.soso.community.voteboard.domain.entity.VotePost;
import com.example.soso.community.voteboard.domain.entity.VotePostLike;
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
public interface VotePostLikeRepository extends JpaRepository<VotePostLike, Long> {

    /**
     * 특정 투표 게시글에 대한 사용자의 좋아요 조회
     */
    Optional<VotePostLike> findByVotePostAndUser(VotePost votePost, Users user);

    /**
     * 특정 투표 게시글에 대한 사용자의 좋아요 존재 여부 확인
     */
    boolean existsByVotePostAndUser(VotePost votePost, Users user);

    /**
     * 특정 투표 게시글의 좋아요 개수 조회
     */
    long countByVotePost(VotePost votePost);

    /**
     * 특정 투표 게시글 ID에 대한 사용자의 좋아요 존재 여부 확인
     */
    @Query("SELECT CASE WHEN COUNT(vpl) > 0 THEN true ELSE false END " +
           "FROM VotePostLike vpl " +
           "WHERE vpl.votePost.id = :votePostId AND vpl.user.id = :userId")
    boolean existsByVotePostIdAndUserId(@Param("votePostId") Long votePostId, @Param("userId") String userId);

    /**
     * 특정 투표 게시글 ID의 좋아요 개수 조회
     */
    @Query("SELECT COUNT(vpl) FROM VotePostLike vpl WHERE vpl.votePost.id = :votePostId")
    long countByVotePostId(@Param("votePostId") Long votePostId);
}
