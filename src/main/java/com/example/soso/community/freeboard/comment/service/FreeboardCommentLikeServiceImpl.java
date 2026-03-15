package com.example.soso.community.freeboard.comment.service;

import com.example.soso.community.freeboard.comment.domain.entity.PostComment;
import com.example.soso.community.freeboard.comment.domain.repository.CommentRepository;
import com.example.soso.community.freeboard.like.domain.CommentLike;
import com.example.soso.community.freeboard.like.repository.CommentLikeRepository;
import com.example.soso.global.exception.domain.CommentErrorCode;
import com.example.soso.global.exception.domain.PostErrorCode;
import com.example.soso.global.exception.domain.UserErrorCode;
import com.example.soso.global.exception.util.CommentException;
import com.example.soso.global.exception.util.PostException;
import com.example.soso.global.exception.util.UserAuthException;
import com.example.soso.global.redis.CommentLikeRedisRepository;
import com.example.soso.users.domain.entity.Users;
import com.example.soso.users.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 자유게시판 댓글 좋아요 비즈니스 로직 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FreeboardCommentLikeServiceImpl implements FreeboardCommentLikeService {

    private final CommentLikeRepository commentLikeRepository;
    private final CommentRepository commentRepository;
    private final UsersRepository usersRepository;
    private final CommentLikeRedisRepository commentLikeRedisRepository;

    @Override
    @Transactional
    public boolean toggleCommentLike(Long freeboardId, Long commentId, String userId) {
        log.info("댓글 좋아요 토글: freeboardId={}, commentId={}, userId={}", freeboardId, commentId, userId);

        // 댓글 존재 확인 (게시글 ID도 함께 검증)
        PostComment comment = commentRepository.findByIdAndPostId(commentId, freeboardId)
                .orElseThrow(() -> new PostException(PostErrorCode.COMMENT_NOT_FOUND));

        // 삭제된 댓글인지 확인
        if (comment.isDeleted()) {
            throw new CommentException(CommentErrorCode.DELETED_COMMENT_CANNOT_BE_MODIFIED);
        }

        // 사용자 존재 확인
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new UserAuthException(UserErrorCode.USER_NOT_FOUND));

        // Redis에서 좋아요 상태 확인
        boolean alreadyLiked = commentLikeRedisRepository.isLiked(commentId, userId);

        if (alreadyLiked) {
            // 좋아요 취소
            commentLikeRedisRepository.removeLike(commentId, userId);

            // DB에서도 좋아요 정보 삭제
            commentLikeRepository.deleteByComment_IdAndUser_Id(commentId, userId);

            log.info("댓글 좋아요 취소 완료: commentId={}, userId={}", commentId, userId);
            return false;
        } else {
            // 좋아요 추가
            commentLikeRedisRepository.addLike(commentId, userId);

            // DB에 좋아요 정보 저장
            CommentLike commentLike = CommentLike.builder()
                    .comment(comment)
                    .user(user)
                    .build();
            commentLikeRepository.save(commentLike);

            log.info("댓글 좋아요 추가 완료: commentId={}, userId={}", commentId, userId);
            return true;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCommentLikedByUser(Long freeboardId, Long commentId, String userId) {
        log.debug("댓글 좋아요 상태 확인: freeboardId={}, commentId={}, userId={}", freeboardId, commentId, userId);

        // 댓글 존재 확인 (게시글 ID도 함께 검증)
        commentRepository.findByIdAndPostId(commentId, freeboardId)
                .orElseThrow(() -> new PostException(PostErrorCode.COMMENT_NOT_FOUND));

        // 사용자 존재 확인
        usersRepository.findById(userId)
                .orElseThrow(() -> new UserAuthException(UserErrorCode.USER_NOT_FOUND));

        // Redis에서 좋아요 상태 확인
        boolean isLiked = commentLikeRedisRepository.isLiked(commentId, userId);

        log.debug("댓글 좋아요 상태: commentId={}, userId={}, isLiked={}", commentId, userId, isLiked);
        return isLiked;
    }
}