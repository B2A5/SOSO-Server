package com.example.soso.community.freeboard.like.service;

import com.example.soso.community.common.likes.repository.PostLikeRepository;
import com.example.soso.community.common.post.repository.PostRepository;
import com.example.soso.community.common.service.AbstractLikeService;
import com.example.soso.users.repository.UsersRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 자유게시판 좋아요 비즈니스 로직 구현체
 * AbstractLikeService를 상속받아 공통 로직을 재사용
 */
@Slf4j
@Service
public class FreeboardLikeServiceImpl extends AbstractLikeService implements FreeboardLikeService {

    public FreeboardLikeServiceImpl(PostLikeRepository postLikeRepository,
                                  PostRepository postRepository,
                                  UsersRepository usersRepository) {
        super(postLikeRepository, postRepository, usersRepository);
    }

    @Override
    public boolean toggleLike(Long postId, String userId) {
        return super.toggleLike(postId, userId);
    }

    @Override
    public boolean isLikedByUser(Long postId, String userId) {
        return super.isLikedByUser(postId, userId);
    }
}