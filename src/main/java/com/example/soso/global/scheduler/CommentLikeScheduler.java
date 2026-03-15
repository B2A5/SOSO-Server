package com.example.soso.global.scheduler;

import com.example.soso.community.freeboard.comment.domain.repository.CommentRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Component
public class CommentLikeScheduler {

    private final RedisTemplate<String, String> redisTemplate;
    private final CommentRepository commentRepository;

    private static final String COMMENT_LIKE_COUNT_PREFIX = "comment:like:count:";

    @Scheduled(cron = "0 0/10 * * * *") // 10분마다
    @Transactional
    public void syncCommentLikeCountToDB() {
        log.info("댓글 좋아요 수 동기화 시작");

        Set<String> keys = redisTemplate.keys(COMMENT_LIKE_COUNT_PREFIX + "*");

        if (keys == null || keys.isEmpty()) return;

        for (String key : keys) {
            try {
                String commentIdStr = key.replace(COMMENT_LIKE_COUNT_PREFIX, "");
                Long commentId = Long.parseLong(commentIdStr);
                String countStr = (String) redisTemplate.opsForValue().get(key);

                if (countStr != null) {
                    long likeCount = Long.parseLong(countStr);
                    commentRepository.updateLikeCount(commentId, likeCount);
                }
            } catch (Exception e) {
                log.error("댓글 좋아요 동기화 실패: {}", key, e);
            }
        }

        log.info("댓글 좋아요 수 동기화 완료");
    }
}
