package com.example.soso.community.common.comment.domain.repository;

import com.example.soso.community.common.comment.domain.entity.Comment;
import com.example.soso.community.common.post.domain.entity.Post;
import com.example.soso.users.domain.entity.Users;
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

    // 자유게시판 댓글 조회를 위한 추가 메서드들
    List<Comment> findByPostIdAndDeletedFalse(Long postId, Pageable pageable);

    List<Comment> findByParentId(Long parentId);

    int countByParentIdAndDeletedFalse(Long parentId);

    // 추상 서비스에서 사용할 메서드들
    Optional<Comment> findByIdAndDeletedFalse(Long commentId);

    int countByPostIdAndDeletedFalse(Long postId);
}
