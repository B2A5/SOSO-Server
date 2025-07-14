package com.example.soso.global.redis;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;


@Repository
@RequiredArgsConstructor
public class RefreshTokenRedisRepository {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String PREFIX = "refresh-token:";

    // 저장: refreshToken → userId
    public void save(String refreshToken, String userId, long ttlMs) {
        redisTemplate.opsForValue().set(PREFIX + refreshToken, userId, Duration.ofMillis(ttlMs));
    }

    // 조회: refreshToken으로 userId 가져오기
    public String getUserIdByRefreshToken(String refreshToken) {
        return redisTemplate.opsForValue().get(PREFIX + refreshToken);
    }

    // 삭제: refreshToken 자체 삭제 (RTR, 로그아웃 등)
    public void delete(String refreshToken) {
        redisTemplate.delete(PREFIX + refreshToken);
    }
}
