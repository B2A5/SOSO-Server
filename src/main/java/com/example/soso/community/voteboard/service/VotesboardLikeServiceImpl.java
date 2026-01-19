package com.example.soso.community.voteboard.service;

import com.example.soso.community.voteboard.domain.entity.Votesboard;
import com.example.soso.community.voteboard.domain.entity.VotesboardLike;
import com.example.soso.community.voteboard.repository.VotesboardLikeRepository;
import com.example.soso.community.voteboard.repository.VotesboardRepository;
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
public class VotesboardLikeServiceImpl implements VotesboardLikeService {

    private final VotesboardLikeRepository votesboardLikeRepository;
    private final VotesboardRepository votesboardRepository;
    private final UsersRepository usersRepository;

    /**
     * 좋아요 토글 (추가/취소)
     */
    @Override
    @Transactional
    public boolean toggleLike(Long votesboardId, String userId) {
        Votesboard votesboard = votesboardRepository.findById(votesboardId)
                .orElseThrow(() -> new IllegalArgumentException("투표 게시글을 찾을 수 없습니다. ID: " + votesboardId));

        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. ID: " + userId));

        // 기존 좋아요가 있는지 확인
        return votesboardLikeRepository.findByVotesboardAndUser(votesboard, user)
                .map(existingLike -> {
                    // 좋아요 취소
                    votesboardLikeRepository.delete(existingLike);
                    return false;
                })
                .orElseGet(() -> {
                    // 좋아요 추가
                    VotesboardLike newLike = VotesboardLike.create(votesboard, user);
                    votesboardLikeRepository.save(newLike);
                    return true;
                });
    }

    /**
     * 사용자가 해당 투표 게시글에 좋아요를 눌렀는지 확인
     */
    @Override
    public boolean isLikedByUser(Long votesboardId, String userId) {
        return votesboardLikeRepository.existsByVotesboardIdAndUserId(votesboardId, userId);
    }

    /**
     * 투표 게시글의 좋아요 개수 조회
     */
    @Override
    public long getLikeCount(Long votesboardId) {
        return votesboardLikeRepository.countByVotesboardId(votesboardId);
    }
}
