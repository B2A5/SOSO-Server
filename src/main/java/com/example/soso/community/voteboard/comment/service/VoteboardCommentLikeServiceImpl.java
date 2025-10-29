package com.example.soso.community.voteboard.comment.service;

import com.example.soso.community.voteboard.comment.domain.entity.VoteboardComment;
import com.example.soso.community.voteboard.comment.domain.entity.VoteboardCommentLike;
import com.example.soso.community.voteboard.comment.domain.repository.VoteboardCommentLikeRepository;
import com.example.soso.community.voteboard.comment.domain.repository.VoteboardCommentRepository;
import com.example.soso.global.exception.domain.PostErrorCode;
import com.example.soso.global.exception.domain.UserErrorCode;
import com.example.soso.global.exception.util.PostException;
import com.example.soso.global.exception.util.UserAuthException;
import com.example.soso.users.domain.entity.Users;
import com.example.soso.users.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 투표 게시판 댓글 좋아요 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoteboardCommentLikeServiceImpl implements VoteboardCommentLikeService {

    private final VoteboardCommentLikeRepository commentLikeRepository;
    private final VoteboardCommentRepository commentRepository;
    private final UsersRepository usersRepository;

    /**
     * 좋아요 토글 (추가/취소)
     */
    @Override
    @Transactional
    public boolean toggleLike(Long commentId, String userId) {
        VoteboardComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new PostException(PostErrorCode.COMMENT_NOT_FOUND));

        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new UserAuthException(UserErrorCode.USER_NOT_FOUND));

        // 기존 좋아요가 있는지 확인
        return commentLikeRepository.findByCommentAndUser(comment, user)
                .map(existingLike -> {
                    // 좋아요 취소
                    commentLikeRepository.delete(existingLike);
                    comment.decrementLikeCount();
                    return false;
                })
                .orElseGet(() -> {
                    // 좋아요 추가
                    VoteboardCommentLike newLike = VoteboardCommentLike.create(comment, user);
                    commentLikeRepository.save(newLike);
                    comment.incrementLikeCount();
                    return true;
                });
    }

    /**
     * 사용자가 해당 댓글에 좋아요를 눌렀는지 확인
     */
    @Override
    public boolean isLikedByUser(Long commentId, String userId) {
        return commentLikeRepository.existsByCommentIdAndUserId(commentId, userId);
    }

    /**
     * 댓글의 좋아요 개수 조회
     */
    @Override
    public long getLikeCount(Long commentId) {
        return commentLikeRepository.countByCommentId(commentId);
    }
}
