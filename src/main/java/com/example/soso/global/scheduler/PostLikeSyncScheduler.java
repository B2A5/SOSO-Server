package com.example.soso.global.scheduler;

import com.example.soso.global.redis.PostLikeRedisRepository;
import com.example.soso.community.freeboard.post.repository.PostRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostLikeSyncScheduler {

    private final PostLikeRedisRepository redisRepository;
    private final PostRepository postRepository;

    @Scheduled(cron = "0 0 1 * * *") // 매 정시 실행 (초 분 시 일 월 요일)
    public void syncLikeCountToDatabase() {
        log.info("[Scheduler] 게시글 좋아요 수 동기화 시작");

        Set<Long> postIds = redisRepository.getAllPostIdsWithLikes();

        for (Long postId : postIds) {
            long redisLikeCount = redisRepository.getLikeCount(postId);
            postRepository.findById(postId).ifPresent(post -> {
                post.updateLikeCount((int) redisLikeCount);
                log.info("게시글 {} - 좋아요 수 {} 반영 완료", postId, redisLikeCount);
            });
        }

        log.info("[Scheduler] 게시글 좋아요 수 동기화 완료");
    }
}