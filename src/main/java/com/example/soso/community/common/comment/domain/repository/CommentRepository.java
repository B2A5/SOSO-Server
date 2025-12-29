package com.example.soso.community.common.comment.domain.repository;

import com.example.soso.community.common.comment.domain.entity.Comment;
import com.example.soso.community.common.post.domain.entity.Post;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Modifying
    @Query("UPDATE Comment c SET c.likeCount = :count WHERE c.id = :commentId")
    void updateLikeCount(@Param("commentId") Long commentId, @Param("count") long count);

    List<Comment> findAllByPost(Post post);

    Optional<Comment> findByIdAndUserId(Long commentId, String userId);

    Optional<Comment> findByIdAndPostId(Long commentId, Long postId);

    @Query("select c.id from Comment c where c.post.id = :postId")
    List<Long> findIdsByPostId(@Param("postId") Long postId);

    // 자유게시판 댓글 조회를 위한 추가 메서드들 (삭제된 댓글 제외)
    List<Comment> findByPostIdAndDeletedFalse(Long postId, Pageable pageable);

    List<Comment> findByParentId(Long parentId);

    int countByParentIdAndDeletedFalse(Long parentId);

    // 추상 서비스에서 사용할 메서드들
    Optional<Comment> findByIdAndDeletedFalse(Long commentId);

    int countByPostIdAndDeletedFalse(Long postId);

    // 총 댓글 수 (삭제된 댓글 포함)
    long countByPostId(Long postId);

    // 커서 기반 페이징을 위한 메서드들 (삭제된 댓글 제외)
    List<Comment> findByPostIdAndDeletedFalseAndCreatedAtBefore(Long postId, LocalDateTime createdAt, Pageable pageable);

    List<Comment> findByPostIdAndDeletedFalseAndCreatedAtAfter(Long postId, LocalDateTime createdAt, Pageable pageable);

    // 소프트 삭제된 댓글도 포함하여 조회하는 메서드들 (댓글 구조 유지용)
    List<Comment> findByPostId(Long postId, Pageable pageable);

    List<Comment> findByPostIdAndCreatedAtBefore(Long postId, LocalDateTime createdAt, Pageable pageable);

    List<Comment> findByPostIdAndCreatedAtAfter(Long postId, LocalDateTime createdAt, Pageable pageable);
}
