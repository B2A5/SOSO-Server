package com.example.soso.config;

import com.example.soso.global.redis.CommentLikeRedisRepository;
import com.example.soso.global.redis.RefreshTokenRedisRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * 테스트용 Redis 설정
 * 실제 Redis 연결 대신 Mock을 사용
 */
@TestConfiguration
public class TestRedisConfig {

    @Bean
    @Primary
    public CommentLikeRedisRepository mockCommentLikeRedisRepository() {
        return new MockCommentLikeRedisRepository();
    }

    @Bean
    @Primary
    public RefreshTokenRedisRepository mockRefreshTokenRedisRepository() {
        return new MockRefreshTokenRedisRepository();
    }

    /**
     * 댓글 좋아요 상태를 메모리에서 추적하는 Mock 구현체
     */
    public static class MockCommentLikeRedisRepository extends CommentLikeRedisRepository {
        private final java.util.Map<String, java.util.Set<String>> likeData = new java.util.HashMap<>();
        private final java.util.Map<Long, Long> likeCountData = new java.util.HashMap<>();

        public MockCommentLikeRedisRepository() {
            super(null); // RedisTemplate을 null로 전달
        }

        private String getKey(Long commentId) {
            return "like:comment:" + commentId;
        }

        @Override
        public void addLike(Long commentId, String userId) {
            String key = getKey(commentId);
            likeData.computeIfAbsent(key, k -> new java.util.HashSet<>()).add(userId);
            likeCountData.put(commentId, likeCountData.getOrDefault(commentId, 0L) + 1);
        }

        @Override
        public void removeLike(Long commentId, String userId) {
            String key = getKey(commentId);
            if (likeData.containsKey(key)) {
                likeData.get(key).remove(userId);
                long currentCount = likeCountData.getOrDefault(commentId, 0L);
                likeCountData.put(commentId, Math.max(0, currentCount - 1));
            }
        }

        @Override
        public boolean isLiked(Long commentId, String userId) {
            String key = getKey(commentId);
            return likeData.containsKey(key) && likeData.get(key).contains(userId);
        }

        @Override
        public long getLikeCount(Long commentId) {
            return likeCountData.getOrDefault(commentId, 0L);
        }
    }

    /**
     * Refresh Token을 메모리에서 추적하는 Mock 구현체
     */
    public static class MockRefreshTokenRedisRepository extends RefreshTokenRedisRepository {
        private final java.util.Map<String, String> tokenData = new java.util.HashMap<>();

        public MockRefreshTokenRedisRepository() {
            super(null); // RedisTemplate을 null로 전달
        }

        @Override
        public void save(String refreshToken, String userId, long ttlMs) {
            tokenData.put(refreshToken, userId);
        }

        // SignupServiceImpl에서 호출하는 메서드
        public void saveByUserId(String userId, String refreshToken, long ttlMs) {
            tokenData.put(refreshToken, userId);
        }

        @Override
        public String getUserIdByRefreshToken(String refreshToken) {
            return tokenData.get(refreshToken);
        }

        @Override
        public void delete(String refreshToken) {
            tokenData.remove(refreshToken);
        }
    }
}