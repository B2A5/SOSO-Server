package com.example.soso.comment.service;


import com.example.soso.comment.domain.dto.CommentLikeResponse;
import com.example.soso.global.redis.CommentLikeRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommentLikeService {

    private final CommentLikeRedisRepository redisRepository;

    public CommentLikeResponse likeComment(Long commentId, String userId) {
        if (!redisRepository.isLiked(commentId, userId)) {
            redisRepository.addLike(commentId, userId);
        }
        long count = redisRepository.getLikeCount(commentId);
        return new CommentLikeResponse(true, count);
    }

    public CommentLikeResponse unlikeComment(Long commentId, String userId) {
        if (redisRepository.isLiked(commentId, userId)) {
            redisRepository.removeLike(commentId, userId);
        }
        long count = redisRepository.getLikeCount(commentId);
        return new CommentLikeResponse(false, count);
    }

    public long getLikeCount(Long commentId) {
        return redisRepository.getLikeCount(commentId);
    }

    public boolean isLiked(Long commentId, String userId) {
        return redisRepository.isLiked(commentId, userId);
    }
}
