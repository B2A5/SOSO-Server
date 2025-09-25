package com.example.soso.community.common.service;

import com.example.soso.community.common.likes.domain.PostLike;
import com.example.soso.community.common.likes.repository.PostLikeRepository;
import com.example.soso.community.common.post.domain.entity.Post;
import com.example.soso.community.common.post.repository.PostRepository;
import com.example.soso.global.exception.domain.PostErrorCode;
import com.example.soso.global.exception.domain.UserErrorCode;
import com.example.soso.global.exception.util.PostException;
import com.example.soso.global.exception.util.UserAuthException;
import com.example.soso.users.domain.entity.Users;
import com.example.soso.users.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * 좋아요 관련 공통 로직을 담은 추상 서비스
 * freeboard와 votesboard에서 공통으로 사용할 수 있도록 설계됨
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractLikeService {

    protected final PostLikeRepository postLikeRepository;
    protected final PostRepository postRepository;
    protected final UsersRepository usersRepository;

    /**
     * 게시글 좋아요 토글 (좋아요/좋아요 취소)
     */
    @Transactional
    public boolean toggleLike(Long postId, String userId) {
        log.info("좋아요 토글: postId={}, userId={}", postId, userId);

        // 게시글과 사용자 존재 확인
        Post post = findPostById(postId);
        Users user = findUserById(userId);

        // 기존 좋아요 확인
        boolean alreadyLiked = postLikeRepository.existsByPost_IdAndUser_Id(postId, userId);

        if (alreadyLiked) {
            // 좋아요 취소
            postLikeRepository.deleteByPostIdAndUserId(postId, userId);
            log.info("좋아요 취소 완료: postId={}, userId={}", postId, userId);

            // 게시글 좋아요 수 업데이트
            updatePostLikeCount(post);
            return false;
        } else {
            // 좋아요 추가
            PostLike postLike = PostLike.builder()
                    .post(post)
                    .user(user)
                    .build();
            postLikeRepository.save(postLike);
            log.info("좋아요 추가 완료: postId={}, userId={}", postId, userId);

            // 게시글 좋아요 수 업데이트
            updatePostLikeCount(post);
            return true;
        }
    }

    /**
     * 사용자가 좋아요한 게시글 ID 목록 조회
     */
    @Transactional(readOnly = true)
    public Set<Long> getLikedPostIds(List<Long> postIds, String userId) {
        return postLikeRepository.findPostIdsByPostIdsAndUserId(postIds, userId);
    }

    /**
     * 특정 게시글에 대한 사용자의 좋아요 여부 확인
     */
    @Transactional(readOnly = true)
    public boolean isLikedByUser(Long postId, String userId) {
        return postLikeRepository.existsByPost_IdAndUser_Id(postId, userId);
    }

    // 공통 유틸리티 메서드들
    protected Post findPostById(Long postId) {
        return postRepository.findByIdAndDeletedFalse(postId)
                .orElseThrow(() -> new PostException(PostErrorCode.NOT_FOUND));
    }

    protected Users findUserById(String userId) {
        return usersRepository.findById(userId)
                .orElseThrow(() -> new UserAuthException(UserErrorCode.USER_NOT_FOUND));
    }

    protected void updatePostLikeCount(Post post) {
        // Redis나 별도 캐싱 시스템이 있다면 여기서 처리
        // 현재는 데이터베이스에서 직접 카운트
        int likeCount = postLikeRepository.countByPost_Id(post.getId());
        post.updateLikeCount(likeCount);
    }
}