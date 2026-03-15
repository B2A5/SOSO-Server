package com.example.soso.community.pollboard.comment.domain.repository;

import com.example.soso.community.pollboard.comment.domain.entity.PollComment;
import com.example.soso.community.pollboard.comment.domain.entity.PollCommentLike;
import com.example.soso.users.domain.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 투표 게시판 댓글 좋아요 Repository
 */
public interface PollCommentLikeRepository extends JpaRepository<PollCommentLike, Long> {

    /**
     * 댓글과 사용자로 좋아요 조회
     */
    Optional<PollCommentLike> findByCommentAndUser(PollComment comment, Users user);

    /**
     * 댓글과 사용자의 좋아요 존재 여부
     */
    boolean existsByCommentAndUser(PollComment comment, Users user);

    /**
     * 댓글의 좋아요 수
     */
    long countByComment(PollComment comment);

    /**
     * 댓글 ID와 사용자 ID로 좋아요 존재 여부 확인
     */
    @Query("SELECT CASE WHEN COUNT(pcl) > 0 THEN true ELSE false END " +
           "FROM PollCommentLike pcl " +
           "WHERE pcl.comment.id = :commentId AND pcl.user.id = :userId")
    boolean existsByCommentIdAndUserId(@Param("commentId") Long commentId, @Param("userId") String userId);

    /**
     * 댓글 ID로 좋아요 수 조회
     */
    @Query("SELECT COUNT(pcl) FROM PollCommentLike pcl WHERE pcl.comment.id = :commentId")
    long countByCommentId(@Param("commentId") Long commentId);
}
