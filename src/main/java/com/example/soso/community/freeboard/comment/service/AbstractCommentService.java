package com.example.soso.community.freeboard.comment.service;

import com.example.soso.community.freeboard.comment.domain.entity.Comment;
import com.example.soso.community.freeboard.comment.domain.repository.CommentRepository;
import com.example.soso.community.freeboard.post.domain.entity.Post;
import com.example.soso.community.freeboard.post.repository.PostRepository;
import com.example.soso.global.exception.domain.CommentErrorCode;
import com.example.soso.global.exception.domain.PostErrorCode;
import com.example.soso.global.exception.domain.UserErrorCode;
import com.example.soso.global.exception.util.CommentException;
import com.example.soso.global.exception.util.PostException;
import com.example.soso.global.exception.util.UserAuthException;
import com.example.soso.users.domain.entity.Users;
import com.example.soso.users.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 댓글 관련 공통 로직을 담은 추상 서비스
 * freeboard와 votesboard에서 공통으로 사용할 수 있도록 설계됨
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractCommentService<T, R, U, S> {

    protected final CommentRepository commentRepository;
    protected final PostRepository postRepository;
    protected final UsersRepository usersRepository;

    /**
     * 댓글 작성 공통 로직
     */
    @Transactional
    public R createComment(T request, String userId, Long postId) {
        log.info("댓글 작성 시작: postId={}, userId={}", postId, userId);

        // 게시글 존재 확인
        Post post = findPostById(postId);

        // 사용자 조회
        Users user = findUserById(userId);

        // 댓글 내용 검증
        validateCommentContent(getContent(request));

        // 부모 댓글 처리
        Comment parentComment = null;
        if (getParentId(request) != null) {
            parentComment = findCommentById(getParentId(request));
            validateReplyDepth(parentComment);
        }

        // 댓글 생성
        Comment comment = Comment.builder()
                .post(post)
                .user(user)
                .parent(parentComment)
                .content(getContent(request))
                .build();

        Comment savedComment = commentRepository.save(comment);

        // 게시글 댓글 수 업데이트
        updatePostCommentCount(post);

        log.info("댓글 작성 완료: commentId={}", savedComment.getId());
        return buildCreateResponse(savedComment.getId());
    }

    /**
     * 커서 기반 댓글 조회 공통 로직
     */
    @Transactional(readOnly = true)
    public U getCommentsByCursor(Long postId, S sortType, int size, String cursor, String userId) {
        log.info("댓글 목록 조회: postId={}, sortType={}, size={}, userId={}",
                postId, sortType, size, userId);

        // 게시글 존재 확인
        findPostById(postId);

        // 페이지 크기 제한 (1-50)
        size = Math.max(1, Math.min(50, size));

        // 정렬 및 페이징 설정
        Sort sort = buildSort(sortType);
        Pageable pageable = PageRequest.of(0, size + 1, sort);

        // 커서 기반 조건 처리
        LocalDateTime cursorTime = parseCursor(cursor);

        // 댓글 조회
        List<Comment> comments = fetchComments(postId, cursorTime, pageable, sortType);

        // 다음 페이지 여부 확인
        boolean hasNext = comments.size() > size;
        if (hasNext) {
            comments.remove(comments.size() - 1);
        }

        // 응답 생성
        String nextCursor = hasNext && !comments.isEmpty() ?
                generateCursor(comments.get(comments.size() - 1)) : null;

        return buildCursorResponse(comments, hasNext, nextCursor, size, userId);
    }

    /**
     * 댓글 수정 공통 로직
     */
    @Transactional
    public R updateComment(Long commentId, Object request, String userId) {
        log.info("댓글 수정 시작: commentId={}, userId={}", commentId, userId);

        Comment comment = findCommentByIdAndUserId(commentId, userId);
        String content = getUpdateContent(request);
        validateCommentContent(content);

        comment.updateContent(content);

        log.info("댓글 수정 완료: commentId={}", commentId);
        return buildCreateResponse(commentId);
    }

    /**
     * 댓글 삭제 공통 로직 (소프트 삭제)
     */
    @Transactional
    public void deleteComment(Long commentId, String userId) {
        log.info("댓글 삭제 시작: commentId={}, userId={}", commentId, userId);

        Comment comment = findCommentByIdAndUserId(commentId, userId);
        comment.delete();

        // 게시글 댓글 수 업데이트
        updatePostCommentCount(comment.getPost());

        log.info("댓글 삭제 완료: commentId={}", commentId);
    }

    // 추상 메서드들 - 구현체에서 정의
    protected abstract String getContent(T request);
    protected abstract Long getParentId(T request);
    protected abstract R buildCreateResponse(Long commentId);
    protected abstract U buildCursorResponse(List<Comment> comments, boolean hasNext,
                                           String nextCursor, int size, String userId);
    protected abstract Sort buildSort(S sortType);
    protected abstract List<Comment> fetchComments(Long postId, LocalDateTime cursorTime,
                                                 Pageable pageable, S sortType);

    // 댓글 수정용 추상 메서드 - 구현체에서 적절한 타입으로 캐스팅하여 반환
    protected abstract String getUpdateContent(Object request);

    // 공통 유틸리티 메서드들
    protected Post findPostById(Long postId) {
        return postRepository.findByIdAndDeletedFalse(postId)
                .orElseThrow(() -> new PostException(PostErrorCode.POST_NOT_FOUND));
    }

    protected Users findUserById(String userId) {
        return usersRepository.findById(userId)
                .orElseThrow(() -> new UserAuthException(UserErrorCode.USER_NOT_FOUND));
    }

    protected Comment findCommentById(Long commentId) {
        return commentRepository.findByIdAndDeletedFalse(commentId)
                .orElseThrow(() -> new CommentException(CommentErrorCode.COMMENT_NOT_FOUND));
    }

    protected Comment findCommentByIdAndUserId(Long commentId, String userId) {
        Comment comment = findCommentById(commentId);
        if (!comment.getUser().getId().equals(userId)) {
            throw new CommentException(CommentErrorCode.COMMENT_ACCESS_DENIED);
        }
        return comment;
    }

    protected void validateCommentContent(String content) {
        if (!StringUtils.hasText(content)) {
            throw new CommentException(CommentErrorCode.COMMENT_CONTENT_EMPTY);
        }
        if (content.trim().length() > 1000) {
            throw new CommentException(CommentErrorCode.COMMENT_CONTENT_TOO_LONG);
        }
    }

    protected void validateReplyDepth(Comment parentComment) {
        if (parentComment.getParent() != null) {
            throw new CommentException(CommentErrorCode.REPLY_DEPTH_EXCEEDED);
        }
    }

    protected void updatePostCommentCount(Post post) {
        int commentCount = commentRepository.countByPostIdAndDeletedFalse(post.getId());
        post.updateCommentCount(commentCount);
    }

    protected LocalDateTime parseCursor(String cursor) {
        if (!StringUtils.hasText(cursor)) {
            return null;
        }

        try {
            String decodedCursor = new String(Base64.getDecoder().decode(cursor));
            return LocalDateTime.parse(decodedCursor, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            log.warn("잘못된 커서 형식: {}", cursor);
            return null;
        }
    }

    protected String generateCursor(Comment comment) {
        String timestamp = comment.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return Base64.getEncoder().encodeToString(timestamp.getBytes());
    }
}
