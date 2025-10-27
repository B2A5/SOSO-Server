package com.example.soso.community.voteboard.service;

import com.example.soso.community.voteboard.domain.dto.*;
import com.example.soso.community.voteboard.domain.entity.VoteStatus;

/**
 * 투표 게시판 비즈니스 로직 인터페이스
 */
public interface VotePostService {

    /**
     * 새 투표 게시글을 작성합니다.
     *
     * @param request 투표 게시글 작성 요청 데이터
     * @param userId 작성자 ID
     * @return 생성된 게시글 ID
     */
    Long createVotePost(VotePostCreateRequest request, String userId);

    /**
     * 투표 게시글 상세 정보를 조회합니다.
     *
     * @param postId 게시글 ID
     * @param userId 조회하는 사용자 ID (null 가능 - 비로그인)
     * @return 투표 게시글 상세 정보
     */
    VotePostDetailResponse getVotePost(Long postId, String userId);

    /**
     * 커서 기반으로 투표 게시글 목록을 조회합니다.
     *
     * @param status 투표 상태 필터 (IN_PROGRESS, COMPLETED, null=전체)
     * @param size 페이지 크기
     * @param cursor 커서 값 (null이면 첫 페이지)
     * @return 게시글 목록과 커서 정보
     */
    VotePostListResponse getVotePostsByCursor(VoteStatus status, int size, Long cursor);

    /**
     * 투표 게시글을 수정합니다 (제목, 내용, 이미지, 투표 설정만 가능).
     * 투표 옵션은 수정 불가능합니다.
     *
     * @param postId 게시글 ID
     * @param request 수정 요청 데이터
     * @param userId 수정하는 사용자 ID
     */
    void updateVotePost(Long postId, VotePostUpdateRequest request, String userId);

    /**
     * 투표 게시글을 삭제합니다 (소프트 삭제).
     *
     * @param postId 게시글 ID
     * @param userId 삭제하는 사용자 ID
     */
    void deleteVotePost(Long postId, String userId);

    /**
     * 투표에 참여합니다.
     *
     * @param postId 투표 게시글 ID
     * @param request 투표 요청 데이터 (선택한 옵션 ID)
     * @param userId 투표하는 사용자 ID
     */
    void vote(Long postId, VoteRequest request, String userId);

    /**
     * 투표를 변경합니다 (재투표).
     * 투표 게시글의 allowRevote가 true인 경우에만 가능합니다.
     *
     * @param postId 투표 게시글 ID
     * @param request 투표 요청 데이터 (새로운 옵션 ID)
     * @param userId 투표하는 사용자 ID
     */
    void changeVote(Long postId, VoteRequest request, String userId);

    /**
     * 투표를 취소합니다.
     * 투표 게시글의 allowRevote가 true인 경우에만 가능합니다.
     *
     * @param postId 투표 게시글 ID
     * @param userId 투표하는 사용자 ID
     */
    void cancelVote(Long postId, String userId);
}
