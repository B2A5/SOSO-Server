package com.example.soso.community.freeboard.service;

import com.example.soso.community.freeboard.domain.dto.*;

/**
 * 자유게시판 댓글 비즈니스 로직 인터페이스
 */
public interface FreeboardCommentService {

    /**
     * 새 댓글을 작성합니다.
     *
     * @param postId 게시글 ID
     * @param request 댓글 작성 요청 데이터
     * @param userId 작성자 ID
     * @return 생성된 댓글 ID
     */
    FreeboardCommentCreateResponse createComment(Long postId, FreeboardCommentCreateRequest request, String userId);

    /**
     * 커서 기반으로 댓글 목록을 조회합니다.
     *
     * @param postId 게시글 ID
     * @param sort 정렬 기준 (LATEST/OLDEST)
     * @param size 페이지 크기
     * @param cursor 커서 값 (null 가능)
     * @param userId 조회하는 사용자 ID
     * @return 댓글 목록과 커서 정보
     */
    FreeboardCommentCursorResponse getCommentsByCursor(Long postId, FreeboardCommentSortType sort, int size, String cursor, String userId);

    /**
     * 댓글을 수정합니다.
     *
     * @param postId 게시글 ID
     * @param commentId 댓글 ID
     * @param request 수정 요청 데이터
     * @param userId 수정하는 사용자 ID
     * @return 수정된 댓글 ID
     */
    FreeboardCommentCreateResponse updateComment(Long postId, Long commentId, FreeboardCommentUpdateRequest request, String userId);

    /**
     * 댓글을 소프트 삭제합니다.
     *
     * @param postId 게시글 ID
     * @param commentId 댓글 ID
     * @param userId 삭제하는 사용자 ID
     */
    void deleteComment(Long postId, Long commentId, String userId);

    /**
     * 댓글을 영구 삭제합니다.
     *
     * @param postId 게시글 ID
     * @param commentId 댓글 ID
     * @param userId 삭제하는 사용자 ID (관리자 권한 필요)
     */
    void hardDeleteComment(Long postId, Long commentId, String userId);
}