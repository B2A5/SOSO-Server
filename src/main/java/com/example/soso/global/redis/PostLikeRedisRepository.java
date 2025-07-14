package com.example.soso.global.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;


@Repository
@RequiredArgsConstructor
public class PostLikeRedisRepository {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String LIKE_SET_KEY_PREFIX = "post:likes:";
    private static final String LIKE_COUNT_KEY_PREFIX = "post:likeCount:";

    public boolean isLiked(Long postId, String userId) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(getSetKey(postId), userId));
    }

    public void addLike(Long postId, String userId) {
        redisTemplate.opsForSet().add(getSetKey(postId), userId);
        redisTemplate.opsForValue().increment(getCountKey(postId));
    }

    public void removeLike(Long postId, String userId) {
        redisTemplate.opsForSet().remove(getSetKey(postId), userId);
        redisTemplate.opsForValue().decrement(getCountKey(postId));
    }

    public long getLikeCount(Long postId) {
        String count = redisTemplate.opsForValue().get(getCountKey(postId));
        if (count == null) {
            return 0;
        }
        return Long.parseLong(count);
    }

    private String getSetKey(Long postId) {
        return LIKE_SET_KEY_PREFIX + postId;
    }

    private String getCountKey(Long postId) {
        return LIKE_COUNT_KEY_PREFIX + postId;
    }
}
