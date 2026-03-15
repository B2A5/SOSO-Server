package com.example.soso.community.freeboard.like.repository;

import com.example.soso.community.freeboard.like.domain.CommentLike;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    @Query("SELECT cl.comment.id FROM CommentLike cl WHERE cl.user.id = :userId AND cl.comment.post.id = :postId")
    List<Long> findCommentIdsByUserIdAndPostId(@Param("userId") String userId, @Param("postId") Long postId);

    boolean existsByComment_IdAndUser_Id(Long commentId, String userId);

    int countByComment_Id(Long commentId);

    void deleteByComment_IdAndUser_Id(Long commentId, String userId);

}
