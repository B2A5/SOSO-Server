package com.example.soso.comment.service;

import com.example.soso.comment.domain.dto.CommentCreateRequest;
import com.example.soso.comment.domain.dto.CommentMapper;
import com.example.soso.comment.domain.dto.PostCommentResponse;
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
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UsersRepository usersRepository;

    @Override
    @Transactional
    public void create(Long postId, String userId, CommentCreateRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostException(PostErrorCode.NOT_FOUND));
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new UserAuthException(UserErrorCode.USER_NOT_FOUND));
        Comment comment = CommentMapper.toEntity(request, post, user);
        commentRepository.save(comment);
    }

    @Override
    @Transactional
    public void update(Long postId, Long commentId, String userId, CommentCreateRequest request) {
        Comment comment = commentRepository.findByIdAndPostId(commentId, postId)
                .orElseThrow(() -> new PostException(PostErrorCode.NOT_FOUND));
        validateCommentOwner(comment.getId(), userId);
        comment.updateContent(request.content());
    }


    @Override
    public List<PostCommentResponse> getcomments(Long postId, String userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostException(PostErrorCode.NOT_FOUND));
        List<Comment> comments = commentRepository.findAllByPost(post);
        List<PostCommentResponse> responseList = new ArrayList<>();
        for (Comment comment : comments) {
            responseList.add(CommentMapper.toResponse(comment));
        }
        return responseList;
    }


    @Override
    @Transactional
    public void delete(Long postId, Long commentId, String userId) {
        Comment comment = commentRepository.findByIdAndPostId(commentId, postId)
                .orElseThrow(() -> new PostException(PostErrorCode.NOT_FOUND));
        validateCommentOwner(comment.getId(), userId);
        commentRepository.delete(comment);
    }

    private void validateCommentOwner(Long commentId, String userId) {
        commentRepository.findByIdAndUserId(commentId, userId)
                .orElseThrow(() -> new IllegalArgumentException("본인이 작성한 댓글만 수정/삭제할 수 있습니다."));
    }
}
