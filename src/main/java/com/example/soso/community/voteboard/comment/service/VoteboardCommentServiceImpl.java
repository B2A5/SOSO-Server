package com.example.soso.community.voteboard.comment.service;

import com.example.soso.community.voteboard.comment.domain.dto.*;
import com.example.soso.community.voteboard.comment.domain.entity.VoteboardComment;
import com.example.soso.community.voteboard.comment.domain.repository.VoteboardCommentLikeRepository;
import com.example.soso.community.voteboard.comment.domain.repository.VoteboardCommentRepository;
import com.example.soso.community.voteboard.domain.entity.VotePost;
import com.example.soso.community.voteboard.repository.VotePostRepository;
import com.example.soso.global.exception.domain.PostErrorCode;
import com.example.soso.global.exception.domain.UserErrorCode;
import com.example.soso.global.exception.util.PostException;
import com.example.soso.global.exception.util.UserAuthException;
import com.example.soso.users.domain.entity.Users;
import com.example.soso.users.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 투표 게시판 댓글 비즈니스 로직 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoteboardCommentServiceImpl implements VoteboardCommentService {

    private final VoteboardCommentRepository commentRepository;
    private final VoteboardCommentLikeRepository commentLikeRepository;
    private final VotePostRepository votePostRepository;
    private final UsersRepository usersRepository;

    @Override
    @Transactional
    public VoteboardCommentCreateResponse createComment(Long votePostId, VoteboardCommentCreateRequest request, String userId) {
        log.info("투표게시판 댓글 작성 시작: votePostId={}, userId={}", votePostId, userId);

        VotePost votePost = findVotePostById(votePostId);
        Users user = findUserById(userId);

        // 부모 댓글 확인 (대댓글인 경우)
        VoteboardComment parent = null;
        if (request.getParentCommentId() != null) {
            parent = findCommentById(request.getParentCommentId());

            // 대댓글의 대댓글은 허용하지 않음
            if (parent.getParent() != null) {
                throw new PostException(PostErrorCode.REPLY_DEPTH_EXCEEDED);
            }
        }

        VoteboardComment comment = VoteboardComment.builder()
                .votePost(votePost)
                .user(user)
                .parent(parent)
                .content(request.getContent())
                .build();

        VoteboardComment savedComment = commentRepository.save(comment);
        log.info("투표게시판 댓글 작성 완료: commentId={}", savedComment.getId());

        return new VoteboardCommentCreateResponse(savedComment.getId());
    }

    @Override
    public VoteboardCommentCursorResponse getCommentsByCursor(Long votePostId, VoteboardCommentSortType sort,
                                                             int size, String cursor, String userId) {
        log.debug("투표게시판 댓글 목록 조회: votePostId={}, sort={}, size={}, userId={}",
                 votePostId, sort, size, userId);

        // 투표 게시글 존재 확인
        findVotePostById(votePostId);

        // 페이징 설정
        Sort sortOrder = buildSort(sort);
        PageRequest pageRequest = PageRequest.of(0, size + 1, sortOrder);

        // 댓글 조회
        List<VoteboardComment> comments = fetchComments(votePostId, cursor, pageRequest, sort);

        // 다음 페이지 존재 여부 확인
        boolean hasNext = comments.size() > size;
        if (hasNext) {
            comments = comments.subList(0, size);
        }

        // 다음 커서 계산
        String nextCursor = hasNext && !comments.isEmpty() ?
                comments.get(comments.size() - 1).getCreatedDate().toString() : null;

        // 댓글 요약 생성
        List<VoteboardCommentCursorResponse.VoteboardCommentSummary> summaries = comments.stream()
                .map(comment -> createCommentSummary(comment, userId))
                .toList();

        // 총 댓글 수
        long total = commentRepository.countByVotePostId(votePostId);

        return VoteboardCommentCursorResponse.builder()
                .comments(summaries)
                .hasNext(hasNext)
                .nextCursor(nextCursor)
                .size(summaries.size())
                .total(total)
                .isAuthorized(userId != null)
                .build();
    }

    @Override
    @Transactional
    public VoteboardCommentCreateResponse updateComment(Long votePostId, Long commentId,
                                                       VoteboardCommentUpdateRequest request, String userId) {
        log.info("투표게시판 댓글 수정 시작: commentId={}, userId={}", commentId, userId);

        VoteboardComment comment = findCommentById(commentId);

        // 권한 확인
        validateCommentAuthor(comment, userId);

        // 삭제된 댓글은 수정 불가
        if (comment.isDeleted()) {
            throw new PostException(PostErrorCode.COMMENT_ALREADY_DELETED);
        }

        comment.updateContent(request.getContent());
        log.info("투표게시판 댓글 수정 완료: commentId={}", commentId);

        return new VoteboardCommentCreateResponse(comment.getId());
    }

    @Override
    @Transactional
    public void deleteComment(Long votePostId, Long commentId, String userId) {
        log.info("투표게시판 댓글 삭제 시작: commentId={}, userId={}", commentId, userId);

        VoteboardComment comment = findCommentById(commentId);

        // 권한 확인
        validateCommentAuthor(comment, userId);

        comment.delete();
        log.info("투표게시판 댓글 삭제 완료: commentId={}", commentId);
    }

    @Override
    @Transactional
    public void hardDeleteComment(Long votePostId, Long commentId, String userId) {
        log.warn("투표게시판 댓글 하드 삭제 시작: commentId={}, userId={}", commentId, userId);

        VoteboardComment comment = findCommentById(commentId);

        // 권한 확인 (관리자 또는 작성자)
        validateCommentAuthor(comment, userId);

        // 자식 댓글도 삭제
        List<VoteboardComment> childComments = commentRepository.findByParentId(commentId);
        commentRepository.deleteAll(childComments);
        commentRepository.delete(comment);

        log.warn("투표게시판 댓글 하드 삭제 완료: commentId={}, deletedChildCount={}",
                commentId, childComments.size());
    }

    // Helper methods

    private VotePost findVotePostById(Long votePostId) {
        return votePostRepository.findByIdAndDeletedFalse(votePostId)
                .orElseThrow(() -> new PostException(PostErrorCode.POST_NOT_FOUND));
    }

    private Users findUserById(String userId) {
        return usersRepository.findById(userId)
                .orElseThrow(() -> new UserAuthException(UserErrorCode.USER_NOT_FOUND));
    }

    private VoteboardComment findCommentById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new PostException(PostErrorCode.COMMENT_NOT_FOUND));
    }

    private void validateCommentAuthor(VoteboardComment comment, String userId) {
        if (!comment.getUser().getId().equals(userId)) {
            throw new UserAuthException(UserErrorCode.UNAUTHORIZED_ACCESS);
        }
    }

    private Sort buildSort(VoteboardCommentSortType sortType) {
        return switch (sortType) {
            case LATEST -> Sort.by(Sort.Direction.DESC, "createdDate");
            case OLDEST -> Sort.by(Sort.Direction.ASC, "createdDate");
        };
    }

    private List<VoteboardComment> fetchComments(Long votePostId, String cursor,
                                                 PageRequest pageRequest, VoteboardCommentSortType sortType) {
        if (cursor == null) {
            // 첫 페이지 - 소프트 삭제된 댓글도 포함 (댓글 구조 유지)
            return commentRepository.findByVotePostId(votePostId, pageRequest);
        } else {
            LocalDateTime cursorTime = LocalDateTime.parse(cursor);
            if (sortType == VoteboardCommentSortType.LATEST) {
                return commentRepository.findByVotePostIdAndCreatedDateBefore(votePostId, cursorTime, pageRequest);
            } else {
                return commentRepository.findByVotePostIdAndCreatedDateAfter(votePostId, cursorTime, pageRequest);
            }
        }
    }

    private VoteboardCommentCursorResponse.VoteboardCommentSummary createCommentSummary(
            VoteboardComment comment, String userId) {

        int replyCount = commentRepository.countByParentIdAndDeletedFalse(comment.getId());
        int depth = comment.getParent() != null ? 1 : 0;

        // 인증 여부
        boolean isAuthorized = userId != null;
        boolean isAuthor = userId != null && comment.getUser().getId().equals(userId);

        // 댓글 좋아요 정보
        Boolean isLiked = isAuthorized ?
                commentLikeRepository.existsByCommentIdAndUserId(comment.getId(), userId) : null;
        Boolean canEdit = isAuthorized ? isAuthor : null;
        Boolean canDelete = isAuthorized ? isAuthor : null;

        return VoteboardCommentCursorResponse.VoteboardCommentSummary.builder()
                .commentId(comment.getId())
                .votePostId(comment.getVotePost().getId())
                .parentCommentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .author(VoteboardCommentCursorResponse.CommentAuthorInfo.builder()
                        .userId(comment.getUser().getId())
                        .nickname(comment.getUser().getNickname())
                        .profileImageUrl(comment.getUser().getProfileImageUrl())
                        .userType(comment.getUser().getUserType())
                        .build())
                .content(comment.isDeleted() ? "삭제된 댓글입니다" : comment.getContent())
                .replyCount(replyCount)
                .likeCount(comment.getLikeCount())
                .depth(depth)
                .deleted(comment.isDeleted())
                .isAuthor(isAuthor)
                .isLiked(isLiked)
                .canEdit(canEdit)
                .canDelete(canDelete)
                .createdAt(comment.getCreatedDate())
                .updatedAt(comment.getLastModifiedDate())
                .build();
    }
}
