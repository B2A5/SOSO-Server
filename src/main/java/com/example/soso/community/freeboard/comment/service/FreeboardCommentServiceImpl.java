package com.example.soso.community.freeboard.comment.service;

import com.example.soso.community.common.comment.domain.entity.Comment;
import com.example.soso.community.common.comment.domain.repository.CommentRepository;
import com.example.soso.community.common.post.domain.entity.Post;
import com.example.soso.community.common.post.repository.PostRepository;
import com.example.soso.community.common.service.AbstractCommentService;
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

    public FreeboardCommentServiceImpl(CommentRepository commentRepository,
                                     PostRepository postRepository,
                                     UsersRepository usersRepository) {
        super(commentRepository, postRepository, usersRepository);
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

        Comment comment = findCommentByIdAndUserId(commentId, userId);
        List<Comment> childComments = commentRepository.findByParentId(commentId);
        int totalDeletedCount = childComments.size() + 1;

        commentRepository.delete(comment);

        Post post = comment.getPost();
        post.updateCommentCount(Math.max(0, post.getCommentCount() - totalDeletedCount));

        log.warn("자유게시판 댓글 하드 삭제 완료: commentId={}, deletedChildCount={}",
                commentId, childComments.size());
    }

    // 추상 메서드 구현
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
    protected FreeboardCommentCursorResponse buildCursorResponse(List<Comment> comments, boolean hasNext,
                                                               String nextCursor, int size, String userId) {
        List<FreeboardCommentCursorResponse.FreeboardCommentSummary> summaries = comments.stream()
                .map(comment -> createCommentSummary(comment, userId))
                .toList();

        return FreeboardCommentCursorResponse.builder()
                .comments(summaries)
                .hasNext(hasNext)
                .nextCursor(nextCursor)
                .size(summaries.size())
                .build();
    }

    @Override
    protected Sort buildSort(FreeboardCommentSortType sortType) {
        return switch (sortType) {
            case LATEST -> Sort.by(Sort.Direction.DESC, "createdDate");
            case OLDEST -> Sort.by(Sort.Direction.ASC, "createdDate");
        };
    }

    @Override
    protected List<Comment> fetchComments(Long postId, LocalDateTime cursorTime,
                                        Pageable pageable, FreeboardCommentSortType sortType) {
        if (cursorTime == null) {
            // 첫 페이지인 경우 - 소프트 삭제된 댓글도 포함하여 조회 (댓글 구조 유지)
            return commentRepository.findByPostId(postId, pageable);
        } else {
            // 커서 기반 페이징 - 소프트 삭제된 댓글도 포함하여 조회
            if (sortType == FreeboardCommentSortType.LATEST) {
                // 최신순: cursorTime보다 이전 댓글들 조회
                return commentRepository.findByPostIdAndCreatedDateBefore(postId, cursorTime, pageable);
            } else {
                // 오래된순: cursorTime보다 이후 댓글들 조회
                return commentRepository.findByPostIdAndCreatedDateAfter(postId, cursorTime, pageable);
            }
        }
    }

    private FreeboardCommentCursorResponse.FreeboardCommentSummary createCommentSummary(Comment comment, String userId) {
        int replyCount = commentRepository.countByParentIdAndDeletedFalse(comment.getId());
        int depth = comment.getParent() != null ? 1 : 0;

        return FreeboardCommentCursorResponse.FreeboardCommentSummary.builder()
                .commentId(comment.getId())
                .postId(comment.getPost().getId())
                .parentCommentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .author(FreeboardCommentCursorResponse.CommentAuthorInfo.builder()
                        .userId(comment.getUser().getId())
                        .nickname(comment.getUser().getNickname())
                        .profileImageUrl(comment.getUser().getProfileImageUrl())
                        .build())
                .content(comment.isDeleted() ? "삭제된 댓글입니다." : comment.getContent())
                .replyCount(replyCount)
                .depth(depth)
                .deleted(comment.isDeleted())
                .isAuthor(comment.getUser().getId().equals(userId))
                .createdAt(comment.getCreatedDate())
                .updatedAt(comment.getLastModifiedDate())
                .build();
    }
}