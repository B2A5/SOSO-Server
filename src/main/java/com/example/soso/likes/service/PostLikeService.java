package com.example.soso.likes.service;

import com.example.soso.global.redis.PostLikeRedisRepository;
import com.example.soso.likes.dto.PostLikeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostLikeService {

    private final PostLikeRedisRepository redisRepository;

    public PostLikeResponse likePost(Long postId, String userId) {
        boolean liked = false;
        if (!redisRepository.isLiked(postId, userId)) {
            redisRepository.addLike(postId, userId);
            liked = true;
        } else {
            liked = true; // 이미 좋아요 눌러져있던 경우도 true
        }
        long count = redisRepository.getLikeCount(postId);
        return new PostLikeResponse(liked, count);
    }

    public PostLikeResponse unlikePost(Long postId, String userId) {
        boolean liked = redisRepository.isLiked(postId, userId);
        if (liked) {
            redisRepository.removeLike(postId, userId);
        }
        long count = redisRepository.getLikeCount(postId);
        return new PostLikeResponse(false, count);
    }


    public boolean isPostLiked(Long postId, String userId) {
        return redisRepository.isLiked(postId, userId);
    }


    public long getPostLikeCount(Long postId) {
        return redisRepository.getLikeCount(postId);
    }
}
