package com.example.soso.community.voteboard.service;

import com.example.soso.community.common.comment.domain.repository.CommentRepository;
import com.example.soso.community.voteboard.domain.dto.*;
import com.example.soso.community.voteboard.dto.VoteboardSortType;
import com.example.soso.community.voteboard.domain.entity.VoteOption;
import com.example.soso.community.voteboard.domain.entity.VotePost;
import com.example.soso.community.voteboard.domain.entity.VotePostImage;
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
public class VotePostServiceImpl implements VotePostService {

    private static final String VOTEBOARD_DIRECTORY = "voteboard";

    private final VotePostRepository votePostRepository;
    private final VoteOptionRepository voteOptionRepository;
    private final VoteResultRepository voteResultRepository;
    private final UsersRepository usersRepository;
    private final CommentRepository commentRepository;
    private final VotePostLikeRepository votePostLikeRepository;
    private final VotePostMapper votePostMapper;
    private final ImageUploadService imageUploadService;

    @Override
    @Transactional
    public Long createVotePost(VotePostCreateRequest request, String userId) {
        log.info("투표 게시글 작성 시작: userId={}, optionCount={}, imageCount={}",
                userId, request.getVoteOptions().size(),
                request.getImages() != null ? request.getImages().size() : 0);

        Users user = findUserById(userId);
        VotePost votePost = votePostMapper.toEntity(request, user);
        VotePost savedPost = votePostRepository.save(votePost);

        // 이미지 업로드 및 저장
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            List<String> imageUrls = imageUploadService.uploadImages(request.getImages(), VOTEBOARD_DIRECTORY);
            saveVotePostImages(savedPost, imageUrls);
        }

        log.info("투표 게시글 작성 완료: postId={}, optionCount={}, imageCount={}",
                savedPost.getId(), savedPost.getVoteOptions().size(), savedPost.getImages().size());
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
        List<VoteResult> userVoteResults = null;
        Boolean isLiked = null;
        if (userId != null) {
            Users user = findUserById(userId);
            userVoteResults = voteResultRepository.findAllByUserAndVotePost(user, votePost);
            isLiked = votePostLikeRepository.existsByVotePostIdAndUserId(postId, userId);
        }

        return votePostMapper.toDetailResponse(votePost, commentCount, userVoteResults, likeCount, isLiked, userId);
    }

    @Override
    public VotePostListResponse getVotePostsByCursor(VoteStatus status, VoteboardSortType sort, int size, String cursor, String userId) {
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
        List<VotePost> posts = votePostRepository.findAllBySortAndCursor(status, sort, cursorId, size);

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
            totalCount = votePostRepository.countByDeletedFalse();
        } else if (status == VoteStatus.IN_PROGRESS) {
            totalCount = votePostRepository.countInProgress(now);
        } else {
            totalCount = votePostRepository.countCompleted(now);
        }

        // 사용자 인증 여부 확인
        boolean isAuthorized = userId != null;

        // DTO 변환
        Users user = userId != null ? findUserById(userId) : null;
        List<VotePostSummaryResponse> summaries = posts.stream()
                .map(post -> {
                    long commentCount = commentRepository.countByPostId(post.getId());
                    long likeCount = votePostLikeRepository.countByVotePostId(post.getId());
                    Boolean isLiked = userId != null
                        ? votePostLikeRepository.existsByVotePostIdAndUserId(post.getId(), userId)
                        : null;
                    Boolean hasVoted = userId != null
                        ? voteResultRepository.existsByUserAndVotePost(user, post)
                        : null;
                    return votePostMapper.toSummaryResponse(post, commentCount, likeCount, isLiked, hasVoted);
                })
                .toList();

        return votePostMapper.toListResponse(summaries, nextCursor, hasNext, totalCount, isAuthorized);
    }

    @Override
    @Transactional
    public void updateVotePost(Long postId, VotePostUpdateRequest request, String userId) {
        log.info("투표 게시글 수정 시작: postId={}, userId={}", postId, userId);

        VotePost votePost = findVotePostById(postId);
        Users user = findUserById(userId);

        // 권한 검증
        validateAuthor(votePost, user);

        // 기존 이미지 삭제 처리
        if (request.getDeleteImageIds() != null && !request.getDeleteImageIds().isEmpty()) {
            deleteVotePostImages(votePost, request.getDeleteImageIds());
        }

        // 새로운 이미지 업로드
        List<String> newImageUrls = Collections.emptyList();
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            // 현재 이미지 개수 + 새 이미지 개수가 4개를 초과하지 않는지 확인
            int currentImageCount = votePost.getImages().size();
            int newImageCount = request.getImages().size();
            if (currentImageCount + newImageCount > imageUploadService.getMaxImageCount()) {
                throw new IllegalArgumentException("총 이미지 개수는 " + imageUploadService.getMaxImageCount() + "개를 초과할 수 없습니다.");
            }

            newImageUrls = imageUploadService.uploadImages(request.getImages(), VOTEBOARD_DIRECTORY);
            saveVotePostImages(votePost, newImageUrls);
        }

        // 게시글 내용 수정
        votePost.updatePost(request.getTitle(), request.getContent(), request.getCategory());

        // 투표 설정 수정 (투표 시작 전에만 가능)
        if (request.getEndTime() != null || request.getAllowRevote() != null || request.getAllowMultipleChoice() != null) {
            LocalDateTime endTime = request.getEndTime() != null ? request.getEndTime() : votePost.getEndTime();
            boolean allowRevote = request.getAllowRevote() != null ? request.getAllowRevote() : votePost.isAllowRevote();
            boolean allowMultipleChoice = request.getAllowMultipleChoice() != null ? request.getAllowMultipleChoice() : votePost.isAllowMultipleChoice();
            votePost.updateVoteSettings(endTime, allowRevote, allowMultipleChoice);
        }

        log.info("투표 게시글 수정 완료: postId={}, newImageCount={}", postId, newImageUrls.size());
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
        log.info("투표 참여 시작: postId={}, userId={}, optionIds={}", postId, userId, request.getVoteOptionIds());

        VotePost votePost = findVotePostById(postId);
        Users user = findUserById(userId);
        List<Long> selectedOptionIds = request.getVoteOptionIds();

        // 투표 진행 중인지 확인
        if (!votePost.isActive()) {
            throw new PostException(PostErrorCode.VOTE_CLOSED);
        }

        // 이미 투표했는지 확인
        if (voteResultRepository.existsByUserAndVotePost(user, votePost)) {
            throw new PostException(PostErrorCode.ALREADY_VOTED);
        }

        // 선택된 옵션 개수 검증
        int totalOptions = votePost.getVoteOptions().size();
        int selectedCount = selectedOptionIds.size();

        if (votePost.isAllowMultipleChoice()) {
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
            VoteOption selectedOption = findVoteOptionById(optionId);

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
        }

        // 총 투표 참여자 수 증가 (한 명이 여러 옵션 선택해도 1명으로 카운트)
        votePost.increaseTotalVotes();

        log.info("투표 참여 완료: postId={}, userId={}, optionIds={}", postId, userId, selectedOptionIds);
    }

    @Override
    @Transactional
    public void changeVote(Long postId, VoteRequest request, String userId) {
        log.info("투표 변경 시작: postId={}, userId={}, newOptionIds={}", postId, userId, request.getVoteOptionIds());

        VotePost votePost = findVotePostById(postId);
        Users user = findUserById(userId);
        List<Long> newOptionIds = request.getVoteOptionIds();

        // 재투표 허용 확인
        if (!votePost.isAllowRevote()) {
            throw new PostException(PostErrorCode.REVOTE_NOT_ALLOWED);
        }

        // 투표 진행 중인지 확인
        if (!votePost.isActive()) {
            throw new PostException(PostErrorCode.VOTE_CLOSED);
        }

        // 기존 투표 결과 조회
        List<VoteResult> existingVotes = voteResultRepository.findAllByUserAndVotePost(user, votePost);
        if (existingVotes.isEmpty()) {
            throw new PostException(PostErrorCode.VOTE_NOT_FOUND);
        }

        // 선택된 옵션 개수 검증
        int totalOptions = votePost.getVoteOptions().size();
        int selectedCount = newOptionIds.size();

        if (votePost.isAllowMultipleChoice()) {
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
        for (VoteResult existingVote : existingVotes) {
            existingVote.getVoteOption().decreaseVoteCount();
            voteResultRepository.delete(existingVote);
        }

        // 새로운 투표 결과 저장
        for (Long optionId : newOptionIds) {
            VoteOption newOption = findVoteOptionById(optionId);

            // 옵션이 해당 투표 게시글의 것인지 확인
            if (!newOption.getVotePost().getId().equals(postId)) {
                throw new PostException(PostErrorCode.INVALID_VOTE_OPTION);
            }

            // 투표 결과 저장
            VoteResult voteResult = VoteResult.builder()
                    .user(user)
                    .votePost(votePost)
                    .voteOption(newOption)
                    .build();
            voteResultRepository.save(voteResult);

            // 투표 수 증가
            newOption.increaseVoteCount();
        }

        log.info("투표 변경 완료: postId={}, userId={}, newOptionIds={}", postId, userId, newOptionIds);
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
        List<VoteResult> voteResults = voteResultRepository.findAllByUserAndVotePost(user, votePost);
        if (voteResults.isEmpty()) {
            throw new PostException(PostErrorCode.VOTE_NOT_FOUND);
        }

        // 모든 투표 결과 삭제 및 투표 수 감소
        for (VoteResult voteResult : voteResults) {
            voteResult.getVoteOption().decreaseVoteCount();
            voteResultRepository.delete(voteResult);
        }

        // 총 투표 참여자 수 감소
        votePost.decreaseTotalVotes();

        log.info("투표 취소 완료: postId={}, userId={}, canceledCount={}", postId, userId, voteResults.size());
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

    private void saveVotePostImages(VotePost votePost, List<String> imageUrls) {
        for (String imageUrl : imageUrls) {
            VotePostImage votePostImage = VotePostImage.builder()
                    .votePost(votePost)
                    .imageUrl(imageUrl)
                    .sequence(votePost.getImages().size())
                    .build();
            votePost.addImage(votePostImage);
        }
    }

    private void deleteVotePostImages(VotePost votePost, List<Long> deleteImageIds) {
        List<VotePostImage> imagesToDelete = votePost.getImages().stream()
                .filter(image -> deleteImageIds.contains(image.getId()))
                .toList();

        for (VotePostImage image : imagesToDelete) {
            imageUploadService.deleteImage(image.getImageUrl());
            votePost.getImages().remove(image);
        }
    }
}
