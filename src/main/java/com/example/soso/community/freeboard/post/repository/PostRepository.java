package com.example.soso.community.freeboard.post.repository;

import com.example.soso.community.common.post.domain.entity.Category;
import com.example.soso.community.freeboard.post.domain.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> , PostRepositoryCustom{

    Optional<Post> findByIdAndUserId(Long postId, String userId);

    // 자유게시판용 추가 메서드들
    Optional<Post> findByIdAndDeletedFalse(Long id);

    Page<Post> findByDeletedFalse(Pageable pageable);

    Page<Post> findByCategoryAndDeletedFalse(Category category, Pageable pageable);

    // 카테고리별 게시글 수 조회
    long countByDeletedFalse();

    long countByCategoryAndDeletedFalse(Category category);
}
