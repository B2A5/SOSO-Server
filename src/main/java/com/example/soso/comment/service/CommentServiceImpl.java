package com.example.soso.comment.service;

import static com.example.soso.comment.domain.dto.CommentMapper.toResponse;

import com.example.soso.comment.domain.dto.CommentCreateRequest;
import com.example.soso.comment.domain.dto.CommentMapper;
import com.example.soso.comment.domain.dto.CommentResponse;
import com.example.soso.comment.domain.entity.Comment;
import com.example.soso.comment.domain.repository.CommentRepository;
import com.example.soso.global.exception.domain.PostErrorCode;
import com.example.soso.global.exception.domain.UserErrorCode;
import com.example.soso.global.exception.util.PostException;
import com.example.soso.global.exception.util.UserAuthException;
import com.example.soso.post.domain.entity.Post;
import com.example.soso.post.repository.PostRepository;
import com.example.soso.users.domain.entity.Users;
import com.example.soso.users.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UsersRepository usersRepository;

    @Override
    @Transactional
    public CommentResponse create(Long postId, String userId, CommentCreateRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostException(PostErrorCode.NOT_FOUND));
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new UserAuthException(UserErrorCode.USER_NOT_FOUND));
        Comment comment = CommentMapper.toEntity(request, post, user);
        Comment saved = commentRepository.save(comment);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public CommentResponse update(Long commentId, String userId, CommentCreateRequest request) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new PostException(PostErrorCode.COMMENT_NOT_FOUND));
        validateCommentOwner(comment, userId);
        comment.updateContent(request.content());
        return toResponse(comment);
    }

    @Override
    @Transactional
    public void delete(Long commentId, String userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new PostException(PostErrorCode.COMMENT_NOT_FOUND));
        validateCommentOwner(comment, userId);
        commentRepository.delete(comment);
    }

    private void validateCommentOwner(Comment comment, String userId) {
        commentRepository.findByIdAndUserId(comment.getId(), userId).orElseThrow(() ->
                new IllegalArgumentException("본인이 작성한 댓글만 수정/삭제할 수 있습니다."));
    }
}
