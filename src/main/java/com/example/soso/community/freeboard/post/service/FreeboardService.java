package com.example.soso.community.freeboard.post.service;

import com.example.soso.community.freeboard.post.domain.dto.*;
import com.example.soso.community.common.post.domain.entity.Category;

/**
 * 자유게시판 비즈니스 로직 인터페이스
 */
public interface FreeboardService {

    /**
     * 새 게시글을 작성합니다.
     *
     * @param request 게시글 작성 요청 데이터
     * @param userId 작성자 ID
     * @return 생성된 게시글 ID
     */
    FreeboardCreateResponse createPost(FreeboardCreateRequest request, String userId);

    /**
     * 게시글 상세 정보를 조회합니다.
     *
     * @param postId 게시글 ID
     * @param userId 조회하는 사용자 ID
     * @return 게시글 상세 정보
     */
    FreeboardDetailResponse getPost(Long postId, String userId);

    /**
     * 커서 기반으로 게시글 목록을 조회합니다.
     *
     * @param category 카테고리 필터 (null 가능)
     * @param sort 정렬 기준
     * @param size 페이지 크기
     * @param cursor 커서 값 (null 가능)
     * @param userId 조회하는 사용자 ID
     * @return 게시글 목록과 커서 정보
     */
    FreeboardCursorResponse getPostsByCursor(Category category, FreeboardSortType sort, int size, String cursor, String userId);

    /**
     * 게시글을 수정합니다.
     *
     * @param postId 게시글 ID
     * @param request 수정 요청 데이터
     * @param userId 수정하는 사용자 ID
     * @return 수정된 게시글 ID
     */
    FreeboardCreateResponse updatePost(Long postId, FreeboardUpdateRequest request, String userId);

    /**
     * 게시글을 소프트 삭제합니다.
     *
     * @param postId 게시글 ID
     * @param userId 삭제하는 사용자 ID
     */
    void deletePost(Long postId, String userId);

    /**
     * 게시글을 영구 삭제합니다.
     *
     * @param postId 게시글 ID
     * @param userId 삭제하는 사용자 ID (관리자 권한 필요)
     */
    void hardDeletePost(Long postId, String userId);
}