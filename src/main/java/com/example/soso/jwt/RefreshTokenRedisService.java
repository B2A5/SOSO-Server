package com.example.soso.jwt;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshTokenRedisService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String PREFIX = "refresh-token:";

    public void save(String userId, String refreshToken, long ttlMs) {
        redisTemplate.opsForValue().set(PREFIX + userId, refreshToken, Duration.ofMillis(ttlMs));
    }

    public String get(String userId) {
        return redisTemplate.opsForValue().get(PREFIX + userId);
    }

    public void delete(String userId) {
        redisTemplate.delete(PREFIX + userId);
    }
}

