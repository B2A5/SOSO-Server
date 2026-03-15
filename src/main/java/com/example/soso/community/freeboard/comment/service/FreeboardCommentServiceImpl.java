package com.example.soso.community.freeboard.comment.service;

import com.example.soso.community.freeboard.comment.domain.entity.PostComment;
import com.example.soso.community.freeboard.comment.domain.repository.CommentRepository;
import com.example.soso.community.freeboard.like.repository.CommentLikeRepository;
import com.example.soso.community.freeboard.post.domain.entity.Post;
import com.example.soso.community.freeboard.post.repository.PostRepository;
import com.example.soso.community.freeboard.comment.domain.dto.*;
import com.example.soso.users.repository.UsersRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 자유게시판 댓글 비즈니스 로직 구현체
 * AbstractCommentService를 상속받아 공통 로직을 재사용하고 추상 메서드를 구현
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class FreeboardCommentServiceImpl
    extends AbstractCommentService<FreeboardCommentCreateRequest, FreeboardCommentCreateResponse,
                                  FreeboardCommentCursorResponse, FreeboardCommentSortType>
    implements FreeboardCommentService {

    private final CommentLikeRepository commentLikeRepository;

    public FreeboardCommentServiceImpl(CommentRepository commentRepository,
                                     PostRepository postRepository,
                                     UsersRepository usersRepository,
                                     CommentLikeRepository commentLikeRepository) {
        super(commentRepository, postRepository, usersRepository);
        this.commentLikeRepository = commentLikeRepository;
    }

    @Override
    @Transactional
    public FreeboardCommentCreateResponse createComment(Long postId, FreeboardCommentCreateRequest request, String userId) {
        return super.createComment(request, userId, postId);
    }

    @Override
    public FreeboardCommentCursorResponse getCommentsByCursor(Long postId, FreeboardCommentSortType sort,
                                                            int size, String cursor, String userId) {
        return super.getCommentsByCursor(postId, sort, size, cursor, userId);
    }

    @Override
    @Transactional
    public FreeboardCommentCreateResponse updateComment(Long postId, Long commentId,
                                                      FreeboardCommentUpdateRequest request, String userId) {
        return super.updateComment(commentId, request, userId);
    }

    @Override
    @Transactional
    public void deleteComment(Long postId, Long commentId, String userId) {
        super.deleteComment(commentId, userId);
    }

    @Override
    @Transactional
    public void hardDeleteComment(Long postId, Long commentId, String userId) {
        log.warn("자유게시판 댓글 하드 삭제 시작: postId={}, commentId={}, userId={}", postId, commentId, userId);

        PostComment comment = findCommentByIdAndUserId(commentId, userId);
        List<PostComment> childComments = commentRepository.findByParentId(commentId);
        int totalDeletedCount = childComments.size() + 1;

        commentRepository.delete(comment);

        Post post = comment.getPost();
        post.updateCommentCount(Math.max(0, post.getCommentCount() - totalDeletedCount));

        log.warn("자유게시판 댓글 하드 삭제 완료: commentId={}, deletedChildCount={}",
                commentId, childComments.size());
    }

    // 추상 메서드 구현
    @Override
    protected String getUpdateContent(Object request) {
        return ((FreeboardCommentUpdateRequest) request).getContent();
    }

    @Override
    protected String getContent(FreeboardCommentCreateRequest request) {
        return request.getContent();
    }

    @Override
    protected Long getParentId(FreeboardCommentCreateRequest request) {
        return request.getParentCommentId();
    }

    @Override
    protected FreeboardCommentCreateResponse buildCreateResponse(Long commentId) {
        return new FreeboardCommentCreateResponse(commentId);
    }

    @Override
    protected FreeboardCommentCursorResponse buildCursorResponse(List<PostComment> comments, boolean hasNext,
                                                               String nextCursor, int size, String userId) {
        List<FreeboardCommentCursorResponse.FreeboardCommentSummary> summaries = comments.stream()
                .map(comment -> createCommentSummary(comment, userId))
                .toList();

        // 첫 번째 댓글의 postId로 총 댓글 수 조회
        long total = 0;
        if (!comments.isEmpty()) {
            Long postId = comments.get(0).getPost().getId();
            total = commentRepository.countByPostId(postId);
        }

        return FreeboardCommentCursorResponse.builder()
                .comments(summaries)
                .hasNext(hasNext)
                .nextCursor(nextCursor)
                .size(summaries.size())
                .total(total)
                .isAuthorized(userId != null)
                .build();
    }

    @Override
    protected Sort buildSort(FreeboardCommentSortType sortType) {
        return switch (sortType) {
            case LATEST -> Sort.by(Sort.Direction.DESC, "createdAt");
            case OLDEST -> Sort.by(Sort.Direction.ASC, "createdAt");
        };
    }

    @Override
    protected List<PostComment> fetchComments(Long postId, LocalDateTime cursorTime,
                                        Pageable pageable, FreeboardCommentSortType sortType) {
        if (cursorTime == null) {
            // 첫 페이지인 경우 - 소프트 삭제된 댓글도 포함하여 조회 (댓글 구조 유지)
            return commentRepository.findByPostId(postId, pageable);
        } else {
            // 커서 기반 페이징 - 소프트 삭제된 댓글도 포함하여 조회
            if (sortType == FreeboardCommentSortType.LATEST) {
                // 최신순: cursorTime보다 이전 댓글들 조회
                return commentRepository.findByPostIdAndCreatedAtBefore(postId, cursorTime, pageable);
            } else {
                // 오래된순: cursorTime보다 이후 댓글들 조회
                return commentRepository.findByPostIdAndCreatedAtAfter(postId, cursorTime, pageable);
            }
        }
    }

    private FreeboardCommentCursorResponse.FreeboardCommentSummary createCommentSummary(PostComment comment, String userId) {
        int replyCount = commentRepository.countByParentIdAndDeletedFalse(comment.getId());
        int depth = comment.getParent() != null ? 1 : 0;

        // 댓글 좋아요 수 조회
        int likeCount = commentLikeRepository.countByComment_Id(comment.getId());

        // 인증 여부 확인
        boolean isAuthorized = userId != null;

        // 작성자 여부 확인
        boolean isAuthor = userId != null && comment.getUser().getId().equals(userId);

        // 인증된 사용자의 댓글 좋아요 여부 확인 (비인증 사용자는 null)
        Boolean isLiked = isAuthorized ?
                commentLikeRepository.existsByComment_IdAndUser_Id(comment.getId(), userId) : null;

        // canEdit, canDelete 설정 (비인증 사용자는 null, 인증 사용자는 작성자 여부에 따라 boolean)
        Boolean canEdit = isAuthorized ? isAuthor : null;
        Boolean canDelete = isAuthorized ? isAuthor : null;

        return FreeboardCommentCursorResponse.FreeboardCommentSummary.builder()
                .commentId(comment.getId())
                .postId(comment.getPost().getId())
                .parentCommentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .author(FreeboardCommentCursorResponse.CommentAuthorInfo.builder()
                        .userId(comment.getUser().getId())
                        .nickname(comment.getUser().getNickname())
                        .profileImageUrl(comment.getUser().getProfileImageUrl())
                        .userType(comment.getUser().getUserType())
                        .build())
                .content(comment.isDeleted() ? "삭제된 댓글입니다." : comment.getContent())
                .replyCount(replyCount)
                .likeCount(likeCount)
                .depth(depth)
                .deleted(comment.isDeleted())
                .isAuthor(isAuthor)
                .isLiked(isLiked)
                .canEdit(canEdit)
                .canDelete(canDelete)
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}