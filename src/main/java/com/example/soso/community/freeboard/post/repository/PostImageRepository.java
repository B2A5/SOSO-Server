package com.example.soso.community.freeboard.post.repository;

import com.example.soso.community.freeboard.post.domain.entity.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {
}
