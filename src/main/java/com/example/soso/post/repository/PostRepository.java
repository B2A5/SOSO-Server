package com.example.soso.post.repository;

import com.example.soso.post.domain.entity.Post;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> , PostRepositoryCustom{

    Optional<Post> findByIdAndUserId(Long postId, String userId);
}
