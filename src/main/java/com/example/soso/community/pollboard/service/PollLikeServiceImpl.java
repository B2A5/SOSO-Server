package com.example.soso.community.pollboard.service;

import com.example.soso.community.pollboard.domain.entity.Poll;
import com.example.soso.community.pollboard.domain.entity.PollLike;
import com.example.soso.community.pollboard.repository.PollLikeRepository;
import com.example.soso.community.pollboard.repository.PollRepository;
import com.example.soso.users.domain.entity.Users;
import com.example.soso.users.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 투표 게시글 좋아요 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PollLikeServiceImpl implements PollLikeService {

    private final PollLikeRepository pollLikeRepository;
    private final PollRepository pollRepository;
    private final UsersRepository usersRepository;

    /**
     * 좋아요 토글 (추가/취소)
     */
    @Override
    @Transactional
    public boolean toggleLike(Long pollId, String userId) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new IllegalArgumentException("투표 게시글을 찾을 수 없습니다. ID: " + pollId));

        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. ID: " + userId));

        // 기존 좋아요가 있는지 확인
        return pollLikeRepository.findByPollAndUser(poll, user)
                .map(existingLike -> {
                    // 좋아요 취소
                    pollLikeRepository.delete(existingLike);
                    return false;
                })
                .orElseGet(() -> {
                    // 좋아요 추가
                    PollLike newLike = PollLike.create(poll, user);
                    pollLikeRepository.save(newLike);
                    return true;
                });
    }

    /**
     * 사용자가 해당 투표 게시글에 좋아요를 눌렀는지 확인
     */
    @Override
    public boolean isLikedByUser(Long pollId, String userId) {
        return pollLikeRepository.existsByPollIdAndUserId(pollId, userId);
    }

    /**
     * 투표 게시글의 좋아요 개수 조회
     */
    @Override
    public long getLikeCount(Long pollId) {
        return pollLikeRepository.countByPollId(pollId);
    }
}
