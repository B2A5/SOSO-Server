package com.example.soso.global.redis;

import com.example.soso.global.exception.domain.PostErrorCode;
import com.example.soso.global.exception.util.PostException;
import java.util.Set;
import java.util.stream.Collectors;
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
            throw new PostException(PostErrorCode.LIKE_COUNT_NOT_FOUND);
        }
        return Long.parseLong(count);
    }

    private String getSetKey(Long postId) {
        return LIKE_SET_KEY_PREFIX + postId;
    }

    private String getCountKey(Long postId) {
        return LIKE_COUNT_KEY_PREFIX + postId;
    }

    public Set<Long> getAllPostIdsWithLikes() {
        Set<String> keys = redisTemplate.keys(LIKE_COUNT_KEY_PREFIX + "*");
        if (keys == null) return Set.of();

        return keys.stream()
                .map(key -> Long.parseLong(key.replace(LIKE_COUNT_KEY_PREFIX, "")))
                .collect(Collectors.toSet());
    }
}
