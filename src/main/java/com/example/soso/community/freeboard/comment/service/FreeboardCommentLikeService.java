package com.example.soso.community.freeboard.comment.service;

/**
 * 자유게시판 댓글 좋아요 비즈니스 로직 인터페이스
 */
public interface FreeboardCommentLikeService {

    /**
     * 댓글 좋아요 토글 (좋아요/좋아요 취소)
     *
     * @param freeboardId 게시글 ID
     * @param commentId 댓글 ID
     * @param userId 사용자 ID
     * @return true: 좋아요 추가, false: 좋아요 취소
     */
    boolean toggleCommentLike(Long freeboardId, Long commentId, String userId);

    /**
     * 특정 댓글에 대한 사용자의 좋아요 여부 확인
     *
     * @param freeboardId 게시글 ID
     * @param commentId 댓글 ID
     * @param userId 사용자 ID
     * @return 좋아요 여부
     */
    boolean isCommentLikedByUser(Long freeboardId, Long commentId, String userId);
}