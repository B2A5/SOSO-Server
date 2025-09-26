package com.example.soso.community.common.post.repository;

import com.example.soso.community.common.post.domain.entity.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {
}
