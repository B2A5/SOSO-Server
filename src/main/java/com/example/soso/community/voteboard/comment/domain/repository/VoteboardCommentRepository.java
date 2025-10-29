package com.example.soso.community.voteboard.comment.domain.repository;

import com.example.soso.community.voteboard.comment.domain.entity.VoteboardComment;
import com.example.soso.community.voteboard.domain.entity.VotePost;
import com.example.soso.users.domain.entity.Users;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 투표 게시판 댓글 Repository
 */
public interface VoteboardCommentRepository extends JpaRepository<VoteboardComment, Long> {

    /**
     * 좋아요 수 업데이트
     */
    @Modifying
    @Query("UPDATE VoteboardComment c SET c.likeCount = :count WHERE c.id = :commentId")
    void updateLikeCount(@Param("commentId") Long commentId, @Param("count") long count);

    /**
     * 투표 게시글의 모든 댓글 조회
     */
    List<VoteboardComment> findAllByVotePost(VotePost votePost);

    /**
     * 댓글 ID와 사용자 ID로 조회
     */
    Optional<VoteboardComment> findByIdAndUserId(Long commentId, String userId);

    /**
     * 댓글 ID와 투표 게시글 ID로 조회
     */
    Optional<VoteboardComment> findByIdAndVotePostId(Long commentId, Long votePostId);

    /**
     * 투표 게시글의 댓글 ID 목록 조회
     */
    @Query("select c.id from VoteboardComment c where c.votePost.id = :votePostId")
    List<Long> findIdsByVotePostId(@Param("votePostId") Long votePostId);

    /**
     * 삭제되지 않은 댓글 조회 (페이징)
     */
    List<VoteboardComment> findByVotePostIdAndDeletedFalse(Long votePostId, Pageable pageable);

    /**
     * 부모 댓글의 자식 댓글 조회
     */
    List<VoteboardComment> findByParentId(Long parentId);

    /**
     * 부모 댓글의 삭제되지 않은 자식 댓글 수
     */
    int countByParentIdAndDeletedFalse(Long parentId);

    /**
     * 삭제되지 않은 댓글 조회
     */
    Optional<VoteboardComment> findByIdAndDeletedFalse(Long commentId);

    /**
     * 투표 게시글의 삭제되지 않은 댓글 수
     */
    int countByVotePostIdAndDeletedFalse(Long votePostId);

    /**
     * 투표 게시글의 총 댓글 수 (삭제된 댓글 포함)
     */
    long countByVotePostId(Long votePostId);

    /**
     * 커서 기반 페이징 - 생성일 이전
     */
    List<VoteboardComment> findByVotePostIdAndDeletedFalseAndCreatedDateBefore(
            Long votePostId, LocalDateTime createdDate, Pageable pageable);

    /**
     * 커서 기반 페이징 - 생성일 이후
     */
    List<VoteboardComment> findByVotePostIdAndDeletedFalseAndCreatedDateAfter(
            Long votePostId, LocalDateTime createdDate, Pageable pageable);

    /**
     * 소프트 삭제된 댓글도 포함하여 조회 (댓글 구조 유지용)
     */
    List<VoteboardComment> findByVotePostId(Long votePostId, Pageable pageable);

    List<VoteboardComment> findByVotePostIdAndCreatedDateBefore(
            Long votePostId, LocalDateTime createdDate, Pageable pageable);

    List<VoteboardComment> findByVotePostIdAndCreatedDateAfter(
            Long votePostId, LocalDateTime createdDate, Pageable pageable);
}
