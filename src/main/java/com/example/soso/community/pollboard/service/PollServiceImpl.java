package com.example.soso.community.pollboard.service;

import com.example.soso.community.pollboard.comment.domain.repository.PollCommentRepository;
import com.example.soso.community.pollboard.domain.dto.*;
import com.example.soso.community.pollboard.dto.PollSortType;
import com.example.soso.community.pollboard.domain.entity.PollOption;
import com.example.soso.community.pollboard.domain.entity.Poll;
import com.example.soso.community.pollboard.domain.entity.PollImage;
import com.example.soso.community.pollboard.domain.entity.Vote;
import com.example.soso.community.pollboard.domain.entity.PollStatus;
import com.example.soso.community.pollboard.repository.PollLikeRepository;
import com.example.soso.community.pollboard.repository.PollOptionRepository;
import com.example.soso.community.pollboard.repository.PollRepository;
import com.example.soso.community.pollboard.repository.VoteRepository;
import com.example.soso.global.exception.domain.PostErrorCode;
import com.example.soso.global.exception.domain.UserErrorCode;
import com.example.soso.global.exception.util.PostException;
import com.example.soso.global.exception.util.UserAuthException;
import com.example.soso.global.image.service.ImageUploadService;
import com.example.soso.users.domain.entity.Users;
import com.example.soso.users.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 투표 게시판 비즈니스 로직 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PollServiceImpl implements PollService {

    private static final String POLL_DIRECTORY = "votesboard";

    private final PollRepository pollRepository;
    private final PollOptionRepository pollOptionRepository;
    private final VoteRepository voteRepository;
    private final UsersRepository usersRepository;
    private final PollCommentRepository pollCommentRepository;
    private final PollLikeRepository pollLikeRepository;
    private final PollMapper pollMapper;
    private final ImageUploadService imageUploadService;

    @Override
    @Transactional
    public Long createPoll(PollCreateRequest request, String userId) {
        log.info("투표 게시글 작성 시작: userId={}, optionCount={}, imageCount={}",
                userId, request.getOptions().size(),
                request.getImages() != null ? request.getImages().size() : 0);

        Users user = findUserById(userId);
        Poll poll = pollMapper.toEntity(request, user);
        Poll savedPost = pollRepository.save(poll);

        // 이미지 업로드 및 저장
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            List<String> imageUrls = imageUploadService.uploadImages(request.getImages(), POLL_DIRECTORY);
            savePollImages(savedPost, imageUrls);
        }

        log.info("투표 게시글 작성 완료: postId={}, optionCount={}, imageCount={}",
                savedPost.getId(), savedPost.getOptions().size(), savedPost.getImages().size());
        return savedPost.getId();
    }

    @Override
    @Transactional
    public PollDetailResponse getPoll(Long postId, String userId) {
        log.debug("투표 게시글 조회: postId={}, userId={}", postId, userId);

        Poll poll = findPollById(postId);

        // 조회수 증가
        poll.increaseViewCount();

        // 댓글 수 조회
        long commentCount = pollCommentRepository.countByPollIdAndDeletedFalse(postId);

        // 좋아요 수 조회
        long likeCount = pollLikeRepository.countByPoll(poll);

        // 사용자의 투표 결과 조회 (비로그인 시 null)
        List<Vote> userVoteResults = null;
        Boolean isLiked = null;
        if (userId != null) {
            Users user = findUserById(userId);
            userVoteResults = voteRepository.findAllByUserAndPoll(user, poll);
            isLiked = pollLikeRepository.existsByPollIdAndUserId(postId, userId);
        }

        return pollMapper.toDetailResponse(poll, commentCount, userVoteResults, likeCount, isLiked, userId);
    }

    @Override
    public PollCursorResponse getPollsByCursor(PollStatus status, PollSortType sort, int size, String cursor, String userId) {
        log.debug("투표 게시글 목록 조회: status={}, sort={}, size={}, cursor={}, userId={}", status, sort, size, cursor, userId);

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

        // Custom Repository를 사용하여 정렬된 목록 조회
        List<Poll> posts = pollRepository.findAllBySortAndCursor(status, sort, cursorId, size);

        // 다음 페이지 존재 여부 확인
        boolean hasNext = posts.size() > size;
        if (hasNext) {
            posts = posts.subList(0, size);
        }

        // 다음 커서 계산 (Long을 String으로 변환)
        String nextCursor = hasNext && !posts.isEmpty()
                ? String.valueOf(posts.get(posts.size() - 1).getId())
                : null;

        // 총 게시글 수 조회 (상태별)
        LocalDateTime now = LocalDateTime.now();
        long totalCount;
        if (status == null) {
            totalCount = pollRepository.countByDeletedFalse();
        } else if (status == PollStatus.IN_PROGRESS) {
            totalCount = pollRepository.countInProgress(now);
        } else {
            totalCount = pollRepository.countCompleted(now);
        }

        // 사용자 인증 여부 확인
        boolean isAuthorized = userId != null;

        // DTO 변환
        Users user = userId != null ? findUserById(userId) : null;
        List<PollSummary> summaries = posts.stream()
                .map(post -> {
                    long commentCount = pollCommentRepository.countByPollIdAndDeletedFalse(post.getId());
                    long likeCount = pollLikeRepository.countByPoll(post);
                    Boolean isLiked = userId != null
                        ? pollLikeRepository.existsByPollIdAndUserId(post.getId(), userId)
                        : null;
                    Boolean hasVoted = userId != null
                        ? voteRepository.existsByUserAndPoll(user, post)
                        : null;
                    return pollMapper.toSummaryResponse(post, commentCount, likeCount, isLiked, hasVoted);
                })
                .toList();

        return pollMapper.toListResponse(summaries, nextCursor, hasNext, totalCount, isAuthorized);
    }

    @Override
    @Transactional
    public void updatePoll(Long postId, PollUpdateRequest request, String userId) {
        log.info("투표 게시글 수정 시작: postId={}, userId={}", postId, userId);

        Poll poll = findPollById(postId);
        Users user = findUserById(userId);

        // 권한 검증
        validateAuthor(poll, user);

        // 기존 이미지 삭제 처리
        if (request.getDeleteImageIds() != null && !request.getDeleteImageIds().isEmpty()) {
            deletePollImages(poll, request.getDeleteImageIds());
        }

        // 새로운 이미지 업로드
        List<String> newImageUrls = Collections.emptyList();
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            // 현재 이미지 개수 + 새 이미지 개수가 4개를 초과하지 않는지 확인
            int currentImageCount = poll.getImages().size();
            int newImageCount = request.getImages().size();
            if (currentImageCount + newImageCount > imageUploadService.getMaxImageCount()) {
                throw new IllegalArgumentException("총 이미지 개수는 " + imageUploadService.getMaxImageCount() + "개를 초과할 수 없습니다.");
            }

            newImageUrls = imageUploadService.uploadImages(request.getImages(), POLL_DIRECTORY);
            savePollImages(poll, newImageUrls);
        }

        // 게시글 내용 수정
        poll.updatePost(request.getTitle(), request.getContent(), request.getCategory());

        // 투표 설정 수정 (투표 시작 전에만 가능)
        if (request.getClosedAt() != null || request.getCanRevote() != null || request.getCanMultiSelect() != null) {
            LocalDateTime closedAt = request.getClosedAt() != null ? request.getClosedAt() : poll.getClosedAt();
            boolean canRevote = request.getCanRevote() != null ? request.getCanRevote() : poll.isCanRevote();
            boolean canMultiSelect = request.getCanMultiSelect() != null ? request.getCanMultiSelect() : poll.isCanMultiSelect();
            poll.updateVoteSettings(closedAt, canRevote, canMultiSelect);
        }

        log.info("투표 게시글 수정 완료: postId={}, newImageCount={}", postId, newImageUrls.size());
    }

    @Override
    @Transactional
    public void deletePoll(Long postId, String userId) {
        log.info("투표 게시글 삭제 시작: postId={}, userId={}", postId, userId);

        Poll poll = findPollById(postId);
        Users user = findUserById(userId);

        // 권한 검증
        validateAuthor(poll, user);

        poll.delete();
        log.info("투표 게시글 삭제 완료: postId={}", postId);
    }

    @Override
    @Transactional
    public void vote(Long postId, VoteRequest request, String userId) {
        log.info("투표 참여 시작: postId={}, userId={}, optionIds={}", postId, userId, request.getVoteOptionIds());

        Poll poll = findPollById(postId);
        Users user = findUserById(userId);
        List<Long> selectedOptionIds = request.getVoteOptionIds();

        // 투표 진행 중인지 확인
        if (!poll.isActive()) {
            throw new PostException(PostErrorCode.VOTE_CLOSED);
        }

        // 이미 투표했는지 확인
        if (voteRepository.existsByUserAndPoll(user, poll)) {
            throw new PostException(PostErrorCode.ALREADY_VOTED);
        }

        // 선택된 옵션 개수 검증
        int totalOptions = poll.getOptions().size();
        int selectedCount = selectedOptionIds.size();

        if (poll.isCanMultiSelect()) {
            // 중복 선택 허용: 최소 1개, 최대 n-1개
            if (selectedCount == 0) {
                throw new PostException(PostErrorCode.INVALID_VOTE_COUNT);
            }
            if (selectedCount >= totalOptions) {
                throw new PostException(PostErrorCode.TOO_MANY_VOTE_OPTIONS);
            }
        } else {
            // 단일 선택: 정확히 1개
            if (selectedCount != 1) {
                throw new PostException(PostErrorCode.SINGLE_VOTE_REQUIRED);
            }
        }

        // 중복 옵션 선택 확인
        if (selectedOptionIds.size() != selectedOptionIds.stream().distinct().count()) {
            throw new PostException(PostErrorCode.DUPLICATE_VOTE_OPTION);
        }

        // 각 옵션에 대해 투표 처리
        for (Long optionId : selectedOptionIds) {
            PollOption selectedOption = findPollOptionById(optionId);

            // 옵션이 해당 투표 게시글의 것인지 확인
            if (!selectedOption.getPoll().getId().equals(postId)) {
                throw new PostException(PostErrorCode.INVALID_VOTE_OPTION);
            }

            // 투표 결과 저장
            Vote vote = Vote.builder()
                    .user(user)
                    .poll(poll)
                    .voteOption(selectedOption)
                    .build();
            voteRepository.save(vote);

            // 투표 수 증가
            selectedOption.increaseVoteCount();
        }

        // 총 투표 참여자 수 증가 (한 명이 여러 옵션 선택해도 1명으로 카운트)
        poll.increaseParticipantCount();

        log.info("투표 참여 완료: postId={}, userId={}, optionIds={}", postId, userId, selectedOptionIds);
    }

    @Override
    @Transactional
    public void changeVote(Long postId, VoteRequest request, String userId) {
        log.info("투표 변경 시작: postId={}, userId={}, newOptionIds={}", postId, userId, request.getVoteOptionIds());

        Poll poll = findPollById(postId);
        Users user = findUserById(userId);
        List<Long> newOptionIds = request.getVoteOptionIds();

        // 재투표 허용 확인
        if (!poll.isCanRevote()) {
            throw new PostException(PostErrorCode.REVOTE_NOT_ALLOWED);
        }

        // 투표 진행 중인지 확인
        if (!poll.isActive()) {
            throw new PostException(PostErrorCode.VOTE_CLOSED);
        }

        // 기존 투표 결과 조회
        List<Vote> existingVotes = voteRepository.findAllByUserAndPoll(user, poll);
        if (existingVotes.isEmpty()) {
            throw new PostException(PostErrorCode.VOTE_NOT_FOUND);
        }

        // 선택된 옵션 개수 검증
        int totalOptions = poll.getOptions().size();
        int selectedCount = newOptionIds.size();

        if (poll.isCanMultiSelect()) {
            // 중복 선택 허용: 최소 1개, 최대 n-1개
            if (selectedCount == 0) {
                throw new PostException(PostErrorCode.INVALID_VOTE_COUNT);
            }
            if (selectedCount >= totalOptions) {
                throw new PostException(PostErrorCode.TOO_MANY_VOTE_OPTIONS);
            }
        } else {
            // 단일 선택: 정확히 1개
            if (selectedCount != 1) {
                throw new PostException(PostErrorCode.SINGLE_VOTE_REQUIRED);
            }
        }

        // 중복 옵션 선택 확인
        if (newOptionIds.size() != newOptionIds.stream().distinct().count()) {
            throw new PostException(PostErrorCode.DUPLICATE_VOTE_OPTION);
        }

        // 기존 선택과 동일한지 확인
        List<Long> existingOptionIds = existingVotes.stream()
                .map(vr -> vr.getVoteOption().getId())
                .sorted()
                .toList();
        List<Long> sortedNewOptionIds = newOptionIds.stream().sorted().toList();

        if (existingOptionIds.equals(sortedNewOptionIds)) {
            log.warn("동일한 옵션으로 재투표 시도: postId={}, userId={}, optionIds={}", postId, userId, newOptionIds);
            return;
        }

        // 기존 투표 결과 삭제 및 투표 수 감소
        for (Vote existingVote : existingVotes) {
            existingVote.getVoteOption().decreaseVoteCount();
            voteRepository.delete(existingVote);
        }

        // 새로운 투표 결과 저장
        for (Long optionId : newOptionIds) {
            PollOption newOption = findPollOptionById(optionId);

            // 옵션이 해당 투표 게시글의 것인지 확인
            if (!newOption.getPoll().getId().equals(postId)) {
                throw new PostException(PostErrorCode.INVALID_VOTE_OPTION);
            }

            // 투표 결과 저장
            Vote vote = Vote.builder()
                    .user(user)
                    .poll(poll)
                    .voteOption(newOption)
                    .build();
            voteRepository.save(vote);

            // 투표 수 증가
            newOption.increaseVoteCount();
        }

        log.info("투표 변경 완료: postId={}, userId={}, newOptionIds={}", postId, userId, newOptionIds);
    }

    @Override
    @Transactional
    public void cancelVote(Long postId, String userId) {
        log.info("투표 취소 시작: postId={}, userId={}", postId, userId);

        Poll poll = findPollById(postId);
        Users user = findUserById(userId);

        // 재투표 허용 확인 (취소도 재투표의 일종)
        if (!poll.isCanRevote()) {
            throw new PostException(PostErrorCode.REVOTE_NOT_ALLOWED);
        }

        // 투표 진행 중인지 확인
        if (!poll.isActive()) {
            throw new PostException(PostErrorCode.VOTE_CLOSED);
        }

        // 기존 투표 결과 조회
        List<Vote> voteResults = voteRepository.findAllByUserAndPoll(user, poll);
        if (voteResults.isEmpty()) {
            throw new PostException(PostErrorCode.VOTE_NOT_FOUND);
        }

        // 모든 투표 결과 삭제 및 투표 수 감소
        for (Vote vote : voteResults) {
            vote.getVoteOption().decreaseVoteCount();
            voteRepository.delete(vote);
        }

        // 총 투표 참여자 수 감소
        poll.decreaseParticipantCount();

        log.info("투표 취소 완료: postId={}, userId={}, canceledCount={}", postId, userId, voteResults.size());
    }

    // 헬퍼 메서드들

    private Users findUserById(String userId) {
        return usersRepository.findById(userId)
                .orElseThrow(() -> new UserAuthException(UserErrorCode.USER_NOT_FOUND));
    }

    private Poll findPollById(Long postId) {
        return pollRepository.findByIdAndDeletedFalse(postId)
                .orElseThrow(() -> new PostException(PostErrorCode.POST_NOT_FOUND));
    }

    private PollOption findPollOptionById(Long optionId) {
        return pollOptionRepository.findById(optionId)
                .orElseThrow(() -> new PostException(PostErrorCode.VOTE_OPTION_NOT_FOUND));
    }

    private void validateAuthor(Poll poll, Users user) {
        if (!poll.getUser().getId().equals(user.getId())) {
            throw new UserAuthException(UserErrorCode.UNAUTHORIZED_ACCESS);
        }
    }

    private void savePollImages(Poll poll, List<String> imageUrls) {
        for (String imageUrl : imageUrls) {
            PollImage pollImage = PollImage.builder()
                    .poll(poll)
                    .imageUrl(imageUrl)
                    .sequence(poll.getImages().size())
                    .build();
            poll.addImage(pollImage);
        }
    }

    private void deletePollImages(Poll poll, List<Long> deleteImageIds) {
        List<PollImage> imagesToDelete = poll.getImages().stream()
                .filter(image -> deleteImageIds.contains(image.getId()))
                .toList();

        for (PollImage image : imagesToDelete) {
            imageUploadService.deleteImage(image.getImageUrl());
            poll.getImages().remove(image);
        }
    }
}
