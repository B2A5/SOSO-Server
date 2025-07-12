package com.example.soso.post.repository;

import com.example.soso.post.domain.entity.Post;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("SELECT p FROM Post p WHERE p.id = :postId AND p.deleted = false")
    Optional<Post> findByIdIfNotDeleted(@Param("postId") Long postId);
}
