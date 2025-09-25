package com.example.soso.community.freeboard.like.service;

/**
 * 자유게시판 좋아요 비즈니스 로직 인터페이스
 */
public interface FreeboardLikeService {

    /**
     * 게시글 좋아요 토글 (좋아요/좋아요 취소)
     *
     * @param postId 게시글 ID
     * @param userId 사용자 ID
     * @return true: 좋아요 추가, false: 좋아요 취소
     */
    boolean toggleLike(Long postId, String userId);

    /**
     * 특정 게시글에 대한 사용자의 좋아요 여부 확인
     *
     * @param postId 게시글 ID
     * @param userId 사용자 ID
     * @return 좋아요 여부
     */
    boolean isLikedByUser(Long postId, String userId);
}