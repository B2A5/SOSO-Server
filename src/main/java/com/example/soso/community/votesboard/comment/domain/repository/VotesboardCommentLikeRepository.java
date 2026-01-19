package com.example.soso.community.votesboard.comment.domain.repository;

import com.example.soso.community.votesboard.comment.domain.entity.VotesboardComment;
import com.example.soso.community.votesboard.comment.domain.entity.VotesboardCommentLike;
import com.example.soso.users.domain.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 투표 게시판 댓글 좋아요 Repository
 */
public interface VotesboardCommentLikeRepository extends JpaRepository<VotesboardCommentLike, Long> {

    /**
     * 댓글과 사용자로 좋아요 조회
     */
    Optional<VotesboardCommentLike> findByCommentAndUser(VotesboardComment comment, Users user);

    /**
     * 댓글과 사용자의 좋아요 존재 여부
     */
    boolean existsByCommentAndUser(VotesboardComment comment, Users user);

    /**
     * 댓글의 좋아요 수
     */
    long countByComment(VotesboardComment comment);

    /**
     * 댓글 ID와 사용자 ID로 좋아요 존재 여부 확인
     */
    @Query("SELECT CASE WHEN COUNT(vcl) > 0 THEN true ELSE false END " +
           "FROM VotesboardCommentLike vcl " +
           "WHERE vcl.comment.id = :commentId AND vcl.user.id = :userId")
    boolean existsByCommentIdAndUserId(@Param("commentId") Long commentId, @Param("userId") String userId);

    /**
     * 댓글 ID로 좋아요 수 조회
     */
    @Query("SELECT COUNT(vcl) FROM VotesboardCommentLike vcl WHERE vcl.comment.id = :commentId")
    long countByCommentId(@Param("commentId") Long commentId);
}
