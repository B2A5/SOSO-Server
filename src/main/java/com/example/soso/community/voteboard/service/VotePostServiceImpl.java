package com.example.soso.community.voteboard.service;

import com.example.soso.community.common.comment.domain.repository.CommentRepository;
import com.example.soso.community.voteboard.domain.dto.*;
import com.example.soso.community.voteboard.domain.entity.VoteOption;
import com.example.soso.community.voteboard.domain.entity.VotePost;
import com.example.soso.community.voteboard.domain.entity.VoteResult;
import com.example.soso.community.voteboard.domain.entity.VoteStatus;
import com.example.soso.community.voteboard.repository.VotePostLikeRepository;
import com.example.soso.community.voteboard.repository.VoteOptionRepository;
import com.example.soso.community.voteboard.repository.VotePostRepository;
import com.example.soso.community.voteboard.repository.VoteResultRepository;
import com.example.soso.global.exception.domain.PostErrorCode;
import com.example.soso.global.exception.domain.UserErrorCode;
import com.example.soso.global.exception.util.PostException;
import com.example.soso.global.exception.util.UserAuthException;
import com.example.soso.users.domain.entity.Users;
import com.example.soso.users.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 투표 게시판 비즈니스 로직 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VotePostServiceImpl implements VotePostService {

    private final VotePostRepository votePostRepository;
    private final VoteOptionRepository voteOptionRepository;
    private final VoteResultRepository voteResultRepository;
    private final UsersRepository usersRepository;
    private final CommentRepository commentRepository;
    private final VotePostLikeRepository votePostLikeRepository;
    private final VotePostMapper votePostMapper;

    @Override
    @Transactional
    public Long createVotePost(VotePostCreateRequest request, String userId) {
        log.info("투표 게시글 작성 시작: userId={}, optionCount={}", userId, request.getVoteOptions().size());

        Users user = findUserById(userId);
        VotePost votePost = votePostMapper.toEntity(request, user);
        VotePost savedPost = votePostRepository.save(votePost);

        log.info("투표 게시글 작성 완료: postId={}, optionCount={}", savedPost.getId(), savedPost.getVoteOptions().size());
        return savedPost.getId();
    }

    @Override
    @Transactional
    public VotePostDetailResponse getVotePost(Long postId, String userId) {
        log.debug("투표 게시글 조회: postId={}, userId={}", postId, userId);

        VotePost votePost = findVotePostById(postId);

        // 조회수 증가
        votePost.increaseViewCount();

        // 댓글 수 조회
        long commentCount = commentRepository.countByPostId(postId);

        // 좋아요 수 조회
        long likeCount = votePostLikeRepository.countByVotePostId(postId);

        // 사용자의 투표 결과 조회 (비로그인 시 null)
        VoteResult userVoteResult = null;
        boolean isLiked = false;
        if (userId != null) {
            Users user = findUserById(userId);
            userVoteResult = voteResultRepository.findByUserAndVotePost(user, votePost).orElse(null);
            isLiked = votePostLikeRepository.existsByVotePostIdAndUserId(postId, userId);
        }

        return votePostMapper.toDetailResponse(votePost, commentCount, userVoteResult, likeCount, isLiked);
    }

    @Override
    public VotePostListResponse getVotePostsByCursor(VoteStatus status, int size, String cursor, String userId) {
        log.debug("투표 게시글 목록 조회: status={}, size={}, cursor={}, userId={}", status, size, cursor, userId);

        // String cursor를 Long으로 파싱
        Long cursorId = null;
        if (cursor != null && !cursor.isEmpty()) {
            try {
                cursorId = Long.parseLong(cursor);
            } catch (NumberFormatException e) {
                log.warn("잘못된 커서 형식: cursor={}", cursor);
                throw new PostException(PostErrorCode.INVALID_CURSOR);
            }
        }

        PageRequest pageRequest = PageRequest.of(0, size + 1);
        LocalDateTime now = LocalDateTime.now();
        List<VotePost> posts;

        // 상태별로 다른 쿼리 실행
        if (status == null) {
            // 전체 조회
            posts = (cursorId == null)
                    ? votePostRepository.findAllWithoutStatus(pageRequest)
                    : votePostRepository.findAllByCursorWithoutStatus(cursorId, pageRequest);
        } else if (status == VoteStatus.IN_PROGRESS) {
            // 진행 중인 투표만
            posts = (cursorId == null)
                    ? votePostRepository.findInProgress(now, pageRequest)
                    : votePostRepository.findInProgressByCursor(cursorId, now, pageRequest);
        } else {
            // 완료된 투표만
            posts = (cursorId == null)
                    ? votePostRepository.findCompleted(now, pageRequest)
                    : votePostRepository.findCompletedByCursor(cursorId, now, pageRequest);
        }

        // 다음 페이지 존재 여부 확인
        boolean hasNext = posts.size() > size;
        if (hasNext) {
            posts = posts.subList(0, size);
        }

        // 다음 커서 계산 (Long을 String으로 변환)
        String nextCursor = hasNext && !posts.isEmpty()
                ? String.valueOf(posts.get(posts.size() - 1).getId())
                : null;

        // DTO 변환
        List<VotePostSummaryResponse> summaries = posts.stream()
                .map(post -> {
                    long commentCount = commentRepository.countByPostId(post.getId());
                    long likeCount = votePostLikeRepository.countByVotePostId(post.getId());
                    boolean isLiked = userId != null && votePostLikeRepository.existsByVotePostIdAndUserId(post.getId(), userId);
                    return votePostMapper.toSummaryResponse(post, commentCount, likeCount, isLiked);
                })
                .toList();

        return votePostMapper.toListResponse(summaries, nextCursor, hasNext);
    }

    @Override
    @Transactional
    public void updateVotePost(Long postId, VotePostUpdateRequest request, String userId) {
        log.info("투표 게시글 수정 시작: postId={}, userId={}", postId, userId);

        VotePost votePost = findVotePostById(postId);
        Users user = findUserById(userId);

        // 권한 검증
        validateAuthor(votePost, user);

        // 게시글 내용 수정
        votePost.updatePost(request.getTitle(), request.getContent(), null);

        // 투표 설정 수정 (투표 시작 전에만 가능)
        if (request.getEndTime() != null || request.getAllowRevote() != null) {
            LocalDateTime endTime = request.getEndTime() != null ? request.getEndTime() : votePost.getEndTime();
            boolean allowRevote = request.getAllowRevote() != null ? request.getAllowRevote() : votePost.isAllowRevote();
            votePost.updateVoteSettings(endTime, allowRevote);
        }

        log.info("투표 게시글 수정 완료: postId={}", postId);
    }

    @Override
    @Transactional
    public void deleteVotePost(Long postId, String userId) {
        log.info("투표 게시글 삭제 시작: postId={}, userId={}", postId, userId);

        VotePost votePost = findVotePostById(postId);
        Users user = findUserById(userId);

        // 권한 검증
        validateAuthor(votePost, user);

        votePost.delete();
        log.info("투표 게시글 삭제 완료: postId={}", postId);
    }

    @Override
    @Transactional
    public void vote(Long postId, VoteRequest request, String userId) {
        log.info("투표 참여 시작: postId={}, userId={}, optionId={}", postId, userId, request.getVoteOptionId());

        VotePost votePost = findVotePostById(postId);
        Users user = findUserById(userId);
        VoteOption selectedOption = findVoteOptionById(request.getVoteOptionId());

        // 투표 진행 중인지 확인
        if (!votePost.isActive()) {
            throw new PostException(PostErrorCode.VOTE_CLOSED);
        }

        // 이미 투표했는지 확인
        if (voteResultRepository.existsByUserAndVotePost(user, votePost)) {
            throw new PostException(PostErrorCode.ALREADY_VOTED);
        }

        // 옵션이 해당 투표 게시글의 것인지 확인
        if (!selectedOption.getVotePost().getId().equals(postId)) {
            throw new PostException(PostErrorCode.INVALID_VOTE_OPTION);
        }

        // 투표 결과 저장
        VoteResult voteResult = VoteResult.builder()
                .user(user)
                .votePost(votePost)
                .voteOption(selectedOption)
                .build();
        voteResultRepository.save(voteResult);

        // 투표 수 증가
        selectedOption.increaseVoteCount();
        votePost.increaseTotalVotes();

        log.info("투표 참여 완료: postId={}, userId={}, optionId={}", postId, userId, request.getVoteOptionId());
    }

    @Override
    @Transactional
    public void changeVote(Long postId, VoteRequest request, String userId) {
        log.info("투표 변경 시작: postId={}, userId={}, newOptionId={}", postId, userId, request.getVoteOptionId());

        VotePost votePost = findVotePostById(postId);
        Users user = findUserById(userId);
        VoteOption newOption = findVoteOptionById(request.getVoteOptionId());

        // 재투표 허용 확인
        if (!votePost.isAllowRevote()) {
            throw new PostException(PostErrorCode.REVOTE_NOT_ALLOWED);
        }

        // 투표 진행 중인지 확인
        if (!votePost.isActive()) {
            throw new PostException(PostErrorCode.VOTE_CLOSED);
        }

        // 기존 투표 결과 조회
        VoteResult voteResult = voteResultRepository.findByUserAndVotePost(user, votePost)
                .orElseThrow(() -> new PostException(PostErrorCode.VOTE_NOT_FOUND));

        // 같은 옵션으로 변경 시도하는 경우
        if (voteResult.getVoteOption().getId().equals(newOption.getId())) {
            log.warn("동일한 옵션으로 재투표 시도: postId={}, userId={}, optionId={}", postId, userId, newOption.getId());
            return;
        }

        // 투표 변경
        voteResult.changeVote(newOption);

        log.info("투표 변경 완료: postId={}, userId={}, newOptionId={}", postId, userId, request.getVoteOptionId());
    }

    @Override
    @Transactional
    public void cancelVote(Long postId, String userId) {
        log.info("투표 취소 시작: postId={}, userId={}", postId, userId);

        VotePost votePost = findVotePostById(postId);
        Users user = findUserById(userId);

        // 재투표 허용 확인 (취소도 재투표의 일종)
        if (!votePost.isAllowRevote()) {
            throw new PostException(PostErrorCode.REVOTE_NOT_ALLOWED);
        }

        // 투표 진행 중인지 확인
        if (!votePost.isActive()) {
            throw new PostException(PostErrorCode.VOTE_CLOSED);
        }

        // 기존 투표 결과 조회
        VoteResult voteResult = voteResultRepository.findByUserAndVotePost(user, votePost)
                .orElseThrow(() -> new PostException(PostErrorCode.VOTE_NOT_FOUND));

        // 투표 수 감소
        voteResult.getVoteOption().decreaseVoteCount();
        votePost.decreaseTotalVotes();

        // 투표 결과 삭제
        voteResultRepository.delete(voteResult);

        log.info("투표 취소 완료: postId={}, userId={}", postId, userId);
    }

    // 헬퍼 메서드들

    private Users findUserById(String userId) {
        return usersRepository.findById(userId)
                .orElseThrow(() -> new UserAuthException(UserErrorCode.USER_NOT_FOUND));
    }

    private VotePost findVotePostById(Long postId) {
        return votePostRepository.findByIdAndDeletedFalse(postId)
                .orElseThrow(() -> new PostException(PostErrorCode.POST_NOT_FOUND));
    }

    private VoteOption findVoteOptionById(Long optionId) {
        return voteOptionRepository.findById(optionId)
                .orElseThrow(() -> new PostException(PostErrorCode.VOTE_OPTION_NOT_FOUND));
    }

    private void validateAuthor(VotePost votePost, Users user) {
        if (!votePost.getUser().getId().equals(user.getId())) {
            throw new UserAuthException(UserErrorCode.UNAUTHORIZED_ACCESS);
        }
    }
}
