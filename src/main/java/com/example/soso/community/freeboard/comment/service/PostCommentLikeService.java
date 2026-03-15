package com.example.soso.community.freeboard.comment.service;

import com.example.soso.community.freeboard.comment.domain.dto.CommentLikeResponse;
import com.example.soso.community.freeboard.comment.domain.repository.CommentRepository;
import com.example.soso.global.exception.domain.PostErrorCode;
import com.example.soso.global.exception.util.PostException;
import com.example.soso.global.redis.CommentLikeRedisRepository;
import com.example.soso.community.freeboard.post.repository.PostRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostCommentLikeService {

    private final CommentLikeRedisRepository redisRepository;
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    public CommentLikeResponse likeComment(Long postId, Long commentId, String userId) {
        validateCommentBelongsToPost(commentId, postId);

        if (!redisRepository.isLiked(commentId, userId)) {
            redisRepository.addLike(commentId, userId);
        }

        long count = redisRepository.getLikeCount(commentId);
        return new CommentLikeResponse(true, count);
    }

    public CommentLikeResponse unlikeComment(Long postId, Long commentId, String userId) {
        validateCommentBelongsToPost(commentId, postId);

        if (redisRepository.isLiked(commentId, userId)) {
            redisRepository.removeLike(commentId, userId);
        }

        long count = redisRepository.getLikeCount(commentId);
        return new CommentLikeResponse(false, count);
    }

    public boolean isLiked(Long postId, Long commentId, String userId) {
        validateCommentBelongsToPost(commentId, postId);
        return redisRepository.isLiked(commentId, userId);
    }

    public List<Long> getLikedCommentIds(Long postId, String userId) {
        if (!postRepository.existsById(postId)) {
            throw new PostException(PostErrorCode.NOT_FOUND);
        }

        List<Long> commentIds = commentRepository.findIdsByPostId(postId);
        return commentIds.stream()
                .filter(commentId -> redisRepository.isLiked(commentId, userId))
                .toList();
    }

    // 게시글-댓글 매핑 검증 메서드
    private void validateCommentBelongsToPost(Long commentId, Long postId) {
        commentRepository.findByIdAndPostId(commentId, postId).orElseThrow(() -> new PostException(PostErrorCode.NOT_FOUND));
        }
    }
