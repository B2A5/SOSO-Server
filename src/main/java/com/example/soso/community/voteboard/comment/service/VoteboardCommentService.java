package com.example.soso.community.voteboard.comment.service;

import com.example.soso.community.voteboard.comment.domain.dto.*;

/**
 * 투표 게시판 댓글 비즈니스 로직 인터페이스
 */
public interface VoteboardCommentService {

    /**
     * 새 댓글을 작성합니다.
     *
     * @param votePostId 투표 게시글 ID
     * @param request 댓글 작성 요청 데이터
     * @param userId 작성자 ID
     * @return 생성된 댓글 ID
     */
    VoteboardCommentCreateResponse createComment(Long votePostId, VoteboardCommentCreateRequest request, String userId);

    /**
     * 커서 기반으로 댓글 목록을 조회합니다.
     *
     * @param votePostId 투표 게시글 ID
     * @param sort 정렬 기준 (LATEST/OLDEST)
     * @param size 페이지 크기
     * @param cursor 커서 값 (null 가능)
     * @param userId 조회하는 사용자 ID (null 가능)
     * @return 댓글 목록과 커서 정보
     */
    VoteboardCommentCursorResponse getCommentsByCursor(Long votePostId, VoteboardCommentSortType sort, int size, String cursor, String userId);

    /**
     * 댓글을 수정합니다.
     *
     * @param votePostId 투표 게시글 ID
     * @param commentId 댓글 ID
     * @param request 수정 요청 데이터
     * @param userId 수정하는 사용자 ID
     * @return 수정된 댓글 ID
     */
    VoteboardCommentCreateResponse updateComment(Long votePostId, Long commentId, VoteboardCommentUpdateRequest request, String userId);

    /**
     * 댓글을 소프트 삭제합니다.
     *
     * @param votePostId 투표 게시글 ID
     * @param commentId 댓글 ID
     * @param userId 삭제하는 사용자 ID
     */
    void deleteComment(Long votePostId, Long commentId, String userId);

    /**
     * 댓글을 영구 삭제합니다.
     *
     * @param votePostId 투표 게시글 ID
     * @param commentId 댓글 ID
     * @param userId 삭제하는 사용자 ID (관리자 권한 필요)
     */
    void hardDeleteComment(Long votePostId, Long commentId, String userId);
}
