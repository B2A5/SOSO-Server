package com.example.soso.global.redis;

import com.example.soso.global.exception.domain.PostErrorCode;
import com.example.soso.global.exception.util.PostException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CommentLikeRedisRepository {

    private final RedisTemplate<String, String> redisTemplate;

    private String getSetKey(Long commentId) {
        return "like:comment:set:" + commentId;
    }

    private String getCountKey(Long commentId) {
        return "like:comment:count:" + commentId;
    }

    // 좋아요 등록
    public void addLike(Long commentId, String userId) {
        redisTemplate.opsForSet().add(getSetKey(commentId), userId);
        redisTemplate.opsForValue().increment(getCountKey(commentId));
    }

    // 좋아요 취소
    public void removeLike(Long commentId, String userId) {
        redisTemplate.opsForSet().remove(getSetKey(commentId), userId);
        redisTemplate.opsForValue().decrement(getCountKey(commentId));
    }

    // 좋아요 여부 확인
    public boolean isLiked(Long commentId, String userId) {
        return Boolean.TRUE.equals(
                redisTemplate.opsForSet().isMember(getSetKey(commentId), userId)
        );
    }

    // 좋아요 수 조회
    public long getLikeCount(Long commentId) {
        String count = (String) redisTemplate.opsForValue().get(getCountKey(commentId));
        if(count == null) {
            throw new PostException(PostErrorCode.LIKE_COUNT_NOT_FOUND);
        }
        return Long.parseLong(count);
    }
}
