package com.example.soso.comment.domain.repository;

import com.example.soso.comment.domain.entity.Comment;
import com.example.soso.post.domain.entity.Post;
import com.example.soso.users.domain.entity.Users;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Modifying
    @Query("UPDATE Comment c SET c.likeCount = :count WHERE c.id = :commentId")
    void updateLikeCount(@Param("commentId") Long commentId, @Param("count") long count);

    List<Comment> findAllByPost(Post post);

    Optional<Comment> findByIdAndUserId(Long id, String userId);
}
