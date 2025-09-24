package com.example.soso.community.freeboard.service;

import com.example.soso.comment.domain.entity.Comment;
import com.example.soso.comment.repository.CommentRepository;
import com.example.soso.community.freeboard.domain.dto.*;
import com.example.soso.global.exception.domain.CommentErrorCode;
import com.example.soso.global.exception.domain.PostErrorCode;
import com.example.soso.global.exception.domain.UserErrorCode;
import com.example.soso.global.exception.util.CommentException;
import com.example.soso.global.exception.util.PostException;
import com.example.soso.global.exception.util.UserAuthException;
import com.example.soso.post.domain.entity.Post;
import com.example.soso.post.repository.PostRepository;
import com.example.soso.users.domain.entity.Users;
import com.example.soso.users.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;

/**
 * 자유게시판 댓글 비즈니스 로직 구현체
 *
 * 주요 책임:
 * - 댓글 CRUD 작업 (대댓글 포함)
 * - 커서 기반 댓글 목록 조회
 * - 권한 검증 및 데이터 검증
 * - 댓글 계층 구조 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FreeboardCommentServiceImpl implements FreeboardCommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UsersRepository usersRepository;

    @Override
    @Transactional
    public FreeboardCommentCreateResponse createComment(Long postId, FreeboardCommentCreateRequest request, String userId) {
        log.info("자유게시판 댓글 작성 시작: postId={}, userId={}, parentCommentId={}",
                postId, userId, request.getParentCommentId());

        // 게시글 존재 확인
        Post post = findPostById(postId);

        // 사용자 조회
        Users user = findUserById(userId);

        // 부모 댓글 검증 (대댓글인 경우)
        Comment parentComment = null;
        if (request.getParentCommentId() != null) {
            parentComment = findCommentById(request.getParentCommentId());

            // 부모 댓글이 같은 게시글에 속하는지 확인
            if (!parentComment.getPost().getId().equals(postId)) {
                throw new CommentException(CommentErrorCode.INVALID_PARENT_COMMENT);
            }

            // 대댓글의 대댓글은 허용하지 않음 (depth 1까지만)
            if (parentComment.getParent() != null) {
                throw new CommentException(CommentErrorCode.REPLY_DEPTH_EXCEEDED);
            }
        }

        // 댓글 엔티티 생성
        Comment comment = Comment.builder()
                .post(post)
                .user(user)
                .parent(parentComment)
                .content(request.getContent())
                .build();

        // 댓글 저장
        Comment savedComment = commentRepository.save(comment);

        // 게시글 댓글 수 증가
        incrementPostCommentCount(post);

        log.info("자유게시판 댓글 작성 완료: commentId={}", savedComment.getId());

        return new FreeboardCommentCreateResponse(savedComment.getId());
    }

    @Override
    public FreeboardCommentCursorResponse getCommentsByCursor(Long postId, FreeboardCommentSortType sort,
                                                            int size, String cursor, String userId) {
        log.debug("자유게시판 댓글 목록 조회: postId={}, sort={}, size={}, userId={}", postId, sort, size, userId);

        // 게시글 존재 확인
        findPostById(postId);

        // 페이지 크기 제한
        if (size > 50) size = 50;
        if (size < 1) size = 20;

        // 커서 파싱
        CommentCursorInfo cursorInfo = parseCommentCursor(cursor, sort);

        // 정렬 및 조건에 따른 댓글 조회
        List<Comment> comments = findCommentsByCursor(postId, sort, size + 1, cursorInfo);

        // 다음 페이지 존재 여부 확인
        boolean hasNext = comments.size() > size;
        if (hasNext) {
            comments = comments.subList(0, size);
        }

        // 응답 DTO 생성
        List<FreeboardCommentCursorResponse.FreeboardCommentSummary> summaries = comments.stream()
                .map(comment -> createCommentSummary(comment, userId))
                .toList();

        // 다음 커서 생성
        String nextCursor = hasNext && !comments.isEmpty()
                ? generateCommentCursor(comments.get(comments.size() - 1), sort)
                : null;

        return FreeboardCommentCursorResponse.builder()
                .comments(summaries)
                .hasNext(hasNext)
                .nextCursor(nextCursor)
                .size(summaries.size())
                .build();
    }

    @Override
    @Transactional
    public FreeboardCommentCreateResponse updateComment(Long postId, Long commentId,
                                                      FreeboardCommentUpdateRequest request, String userId) {
        log.info("자유게시판 댓글 수정 시작: postId={}, commentId={}, userId={}", postId, commentId, userId);

        // 댓글 조회 및 권한 확인
        Comment comment = findCommentByIdAndUserId(commentId, userId);

        // 게시글 매칭 확인
        if (!comment.getPost().getId().equals(postId)) {
            throw new CommentException(CommentErrorCode.COMMENT_POST_MISMATCH);
        }

        // 삭제된 댓글 수정 방지
        if (comment.isDeleted()) {
            throw new CommentException(CommentErrorCode.DELETED_COMMENT_CANNOT_BE_MODIFIED);
        }

        // 댓글 내용 업데이트
        comment.updateContent(request.getContent());

        log.info("자유게시판 댓글 수정 완료: commentId={}", commentId);

        return new FreeboardCommentCreateResponse(commentId);
    }

    @Override
    @Transactional
    public void deleteComment(Long postId, Long commentId, String userId) {
        log.info("자유게시판 댓글 소프트 삭제: postId={}, commentId={}, userId={}", postId, commentId, userId);

        // 댓글 조회 및 권한 확인
        Comment comment = findCommentByIdAndUserId(commentId, userId);

        // 게시글 매칭 확인
        if (!comment.getPost().getId().equals(postId)) {
            throw new CommentException(CommentErrorCode.COMMENT_POST_MISMATCH);
        }

        // 소프트 삭제
        comment.delete();

        // 게시글 댓글 수 감소
        decrementPostCommentCount(comment.getPost());

        log.info("자유게시판 댓글 소프트 삭제 완료: commentId={}", commentId);
    }

    @Override
    @Transactional
    public void hardDeleteComment(Long postId, Long commentId, String userId) {
        log.warn("자유게시판 댓글 하드 삭제 시작: postId={}, commentId={}, userId={}", postId, commentId, userId);

        // TODO: 관리자 권한 확인 로직 추가
        Comment comment = findCommentByIdAndUserId(commentId, userId);

        // 게시글 매칭 확인
        if (!comment.getPost().getId().equals(postId)) {
            throw new CommentException(CommentErrorCode.COMMENT_POST_MISMATCH);
        }

        // 자식 댓글들도 함께 삭제
        List<Comment> childComments = commentRepository.findByParentId(commentId);
        int totalDeletedCount = childComments.size() + 1;

        // 데이터베이스에서 완전 삭제
        commentRepository.delete(comment);

        // 게시글 댓글 수 조정
        Post post = comment.getPost();
        post.updateCommentCount(Math.max(0, post.getCommentCount() - totalDeletedCount));

        log.warn("자유게시판 댓글 하드 삭제 완료: commentId={}, deletedChildCount={}",
                commentId, childComments.size());
    }

    // === 내부 헬퍼 메서드들 ===

    private Post findPostById(Long postId) {
        return postRepository.findByIdAndDeletedFalse(postId)
                .orElseThrow(() -> new PostException(PostErrorCode.POST_NOT_FOUND));
    }

    private Users findUserById(String userId) {
        return usersRepository.findById(userId)
                .orElseThrow(() -> new UserAuthException(UserErrorCode.USER_NOT_FOUND));
    }

    private Comment findCommentById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentException(CommentErrorCode.COMMENT_NOT_FOUND));
    }

    private Comment findCommentByIdAndUserId(Long commentId, String userId) {
        Comment comment = findCommentById(commentId);

        if (!comment.getUser().getId().equals(userId)) {
            throw new CommentException(CommentErrorCode.COMMENT_ACCESS_DENIED);
        }

        return comment;
    }

    private void incrementPostCommentCount(Post post) {
        post.updateCommentCount(post.getCommentCount() + 1);
    }

    private void decrementPostCommentCount(Post post) {
        post.updateCommentCount(Math.max(0, post.getCommentCount() - 1));
    }

    private CommentCursorInfo parseCommentCursor(String cursor, FreeboardCommentSortType sort) {
        if (!StringUtils.hasText(cursor)) {
            return new CommentCursorInfo(null, null);
        }

        try {
            // Base64 디코딩 후 JSON 파싱 (간단 구현)
            String decodedCursor = new String(Base64.getDecoder().decode(cursor));
            // 실제로는 JSON 라이브러리 사용 권장
            return new CommentCursorInfo(null, null);
        } catch (Exception e) {
            log.warn("댓글 커서 파싱 실패: cursor={}, error={}", cursor, e.getMessage());
            return new CommentCursorInfo(null, null);
        }
    }

    private List<Comment> findCommentsByCursor(Long postId, FreeboardCommentSortType sort,
                                             int size, CommentCursorInfo cursorInfo) {
        // 정렬 순서 생성
        Sort sortOrder = createCommentSort(sort);
        Pageable pageable = PageRequest.of(0, size, sortOrder);

        // 게시글의 댓글 조회 (부모 댓글과 대댓글 모두)
        return commentRepository.findByPostIdAndDeletedFalse(postId, pageable);
    }

    private Sort createCommentSort(FreeboardCommentSortType sortType) {
        return switch (sortType) {
            case LATEST -> Sort.by(Sort.Direction.DESC, "createdAt");
            case OLDEST -> Sort.by(Sort.Direction.ASC, "createdAt");
        };
    }

    private FreeboardCommentCursorResponse.FreeboardCommentSummary createCommentSummary(Comment comment, String userId) {
        // 대댓글 개수 조회
        int replyCount = commentRepository.countByParentIdAndDeletedFalse(comment.getId());

        // 댓글 깊이 계산
        int depth = comment.getParent() != null ? 1 : 0;

        return FreeboardCommentCursorResponse.FreeboardCommentSummary.builder()
                .commentId(comment.getId())
                .postId(comment.getPost().getId())
                .parentCommentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .author(FreeboardCommentCursorResponse.AuthorInfo.builder()
                        .userId(comment.getUser().getId())
                        .nickname(comment.getUser().getNickname())
                        .profileImageUrl(comment.getUser().getProfileImageUrl())
                        .build())
                .content(comment.isDeleted() ? "삭제된 댓글입니다." : comment.getContent())
                .replyCount(replyCount)
                .depth(depth)
                .isDeleted(comment.isDeleted())
                .isAuthor(comment.getUser().getId().equals(userId))
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }

    private String generateCommentCursor(Comment comment, FreeboardCommentSortType sort) {
        String cursorValue = switch (sort) {
            case LATEST, OLDEST -> comment.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        };

        String cursorData = String.format("{\"id\":%d,\"sortValue\":\"%s\"}", comment.getId(), cursorValue);
        return Base64.getEncoder().encodeToString(cursorData.getBytes());
    }

    // 댓글 커서 정보를 담는 내부 클래스
    private record CommentCursorInfo(Long lastId, String lastValue) {
    }
}