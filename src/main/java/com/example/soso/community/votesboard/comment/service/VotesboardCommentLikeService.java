package com.example.soso.community.votesboard.comment.service;

/**
 * 투표 게시판 댓글 좋아요 서비스 인터페이스
 */
public interface VotesboardCommentLikeService {

    /**
     * 댓글 좋아요 토글 (추가/취소)
     *
     * @param commentId 댓글 ID
     * @param userId 사용자 ID
     * @return true: 좋아요 추가, false: 좋아요 취소
     */
    boolean toggleLike(Long commentId, String userId);

    /**
     * 사용자가 해당 댓글에 좋아요를 눌렀는지 확인
     *
     * @param commentId 댓글 ID
     * @param userId 사용자 ID
     * @return true: 좋아요 상태, false: 좋아요 안함
     */
    boolean isLikedByUser(Long commentId, String userId);

    /**
     * 댓글의 좋아요 개수 조회
     *
     * @param commentId 댓글 ID
     * @return 좋아요 개수
     */
    long getLikeCount(Long commentId);
}
