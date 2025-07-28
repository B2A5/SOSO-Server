package com.example.soso.post.service;

import com.example.soso.comment.domain.repository.CommentRepository;
import com.example.soso.global.exception.domain.UserErrorCode;
import com.example.soso.global.exception.util.UserAuthException;
import com.example.soso.global.s3.GcsService;
import com.example.soso.global.exception.domain.PostErrorCode;
import com.example.soso.global.exception.util.PostException;
import com.example.soso.likes.repository.PostLikeRepository;
import com.example.soso.post.domain.dto.PostCreateRequest;
import com.example.soso.post.domain.dto.PostMapper;
import com.example.soso.post.domain.dto.PostResponse;
import com.example.soso.post.domain.dto.PostUpdateRequest;
import com.example.soso.post.domain.entity.Post;
import com.example.soso.post.domain.entity.PostImage;
import com.example.soso.post.repository.PostImageRepository;
import com.example.soso.post.repository.PostRepository;
import com.example.soso.users.domain.entity.Users;
import com.example.soso.users.repository.UsersRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final GcsService gcsService;
    private final UsersRepository usersRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentRepository commentRepository;

    @Override
    @Transactional
    public Long createPost(PostCreateRequest request, String userId) {
        Users users = getUserById(userId);

        Post post = PostMapper.toEntity(request, users);
        postRepository.save(post);

        List<PostImage> postImages = createPostImages(request.images(), post);
        postImages.forEach(image -> {
            postImageRepository.save(image);
            post.addImage(image);
        });

        return post.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public PostResponse getPost(Long postId, String userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostException(PostErrorCode.NOT_FOUND));
        boolean isLiked = postLikeRepository.existsByPostIdAndUserId(postId, userId);

        return PostMapper.toResponse(post, isLiked);
    }

    @Override
    @Transactional
    public Long updatePost(Long postId, PostUpdateRequest request, String userId) {
        Post post = postRepository.findByIdAndUserId(postId, userId)
                .orElseThrow(() -> new PostException(PostErrorCode.FORBIDDEN));

        post.getImages().forEach(image -> gcsService.deleteImage(image.getImageUrl()));
        post.getImages().clear();

        List<PostImage> postImages = createPostImages(request.images(), post);
        postImages.forEach(post::addImage);

        post.update(request.title(), request.content(), request.category(), postImages);
        return post.getId();
    }

    @Override
    @Transactional
    public void deletePost(Long postId, String userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostException(PostErrorCode.NOT_FOUND));

        postRepository.findByIdAndUserId(postId, userId)
                .orElseThrow(() -> new PostException(PostErrorCode.FORBIDDEN));

        post.getImages().forEach(image -> gcsService.deleteImage(image.getImageUrl()));
        post.getImages().clear();

        post.delete(); // soft delete
    }

    @Override
    @Transactional
    public void hardDeletePost(Long postId, String userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostException(PostErrorCode.NOT_FOUND));

        postRepository.findByIdAndUserId(postId, userId)
                .orElseThrow(() -> new PostException(PostErrorCode.FORBIDDEN));

        post.getImages().forEach(image -> gcsService.deleteImage(image.getImageUrl()));
        postRepository.delete(post); // 실제 DB 삭제
    }

    /**
     * 이미지 리스트가 null 또는 비어 있을 경우 안전하게 처리
     */
    private List<PostImage> createPostImages(List<MultipartFile> images, Post post) {
        List<PostImage> postImages = new ArrayList<>();

        if (images == null || images.isEmpty()) {
            return postImages;
        }

        for (int i = 0; i < images.size(); i++) {
            MultipartFile file = images.get(i);
            String imageUrl = gcsService.uploadImage(file, "posts");

            PostImage postImage = PostImage.builder()
                    .imageUrl(imageUrl)
                    .sequence(i)
                    .post(post)
                    .build();

            postImages.add(postImage);
        }

        return postImages;
    }

    private Users getUserById(String userId) {
        return usersRepository.findById(userId)
                .orElseThrow(() -> new UserAuthException(UserErrorCode.USER_NOT_FOUND));
    }
}
