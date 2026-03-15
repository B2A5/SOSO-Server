package com.example.soso.community.pollboard.service;

/**
 * 투표 게시글 좋아요 서비스 인터페이스
 */
public interface PollLikeService {

    /**
     * 좋아요 토글 (추가/취소)
     *
     * @param pollId 투표 게시글 ID
     * @param userId 사용자 ID
     * @return 좋아요 추가 시 true, 취소 시 false
     */
    boolean toggleLike(Long pollId, String userId);

    /**
     * 사용자가 해당 투표 게시글에 좋아요를 눌렀는지 확인
     *
     * @param pollId 투표 게시글 ID
     * @param userId 사용자 ID
     * @return 좋아요를 눌렀으면 true
     */
    boolean isLikedByUser(Long pollId, String userId);

    /**
     * 투표 게시글의 좋아요 개수 조회
     *
     * @param pollId 투표 게시글 ID
     * @return 좋아요 개수
     */
    long getLikeCount(Long pollId);
}
