package com.example.soso.post.service;

import com.example.soso.global.exception.domain.UserErrorCode;
import com.example.soso.global.exception.util.BaseException;
import com.example.soso.global.exception.util.UserAuthException;
import com.example.soso.global.s3.S3Service;
import com.example.soso.global.exception.domain.PostErrorCode;
import com.example.soso.global.exception.util.PostException;
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
    private final S3Service s3Service;
    private final UsersRepository usersRepository;

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
    public PostResponse getPost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostException(PostErrorCode.NOT_FOUND));
        return PostMapper.toResponse(post);
    }

    @Override
    @Transactional
    public Long updatePost(Long postId, PostUpdateRequest request, String userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostException(PostErrorCode.NOT_FOUND));

        Users users = getUserById(userId);

        if (!post.getUser().getId().equals(users.getId())) {
            throw new PostException(PostErrorCode.FORBIDDEN);
        }

        post.getImages().forEach(image -> s3Service.deleteImage(image.getImageUrl()));
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

        post.getImages().forEach(image -> s3Service.deleteImage(image.getImageUrl()));
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

        post.getImages().forEach(image -> s3Service.deleteImage(image.getImageUrl()));
        postRepository.delete(post); // 실제 DB 삭제
    }

    /**
     * 이미지 리스트를 S3에 업로드하고, PostImage 리스트를 생성한다.
     */
    private List<PostImage> createPostImages(List<MultipartFile> images, Post post) {
        List<PostImage> postImages = new ArrayList<>();

        for (int i = 0; i < images.size(); i++) {
            MultipartFile file = images.get(i);
            String imageUrl = s3Service.uploadImage(file, "posts");

            PostImage postImage = PostImage.builder()
                    .imageUrl(imageUrl)
                    .sequence(i)
                    .post(post)
                    .build();

            postImages.add(postImage);
        }

        return postImages;
    }

    /**
     * 사용자 ID로 사용자 엔티티를 조회한다.
     */
    private Users getUserById(String userId) {
        return usersRepository.findById(userId)
                .orElseThrow(() -> new UserAuthException(UserErrorCode.USER_NOT_FOUND));
    }
}
