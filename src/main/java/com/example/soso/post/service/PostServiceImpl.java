package com.example.soso.post.service;

import com.example.soso.global.config.S3Service;
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
import com.example.soso.post.service.PostService;
import com.example.soso.users.domain.entity.Users;
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

    @Override
    @Transactional
    public Long createPost(PostCreateRequest request, Users user) {
        Post post = PostMapper.toEntity(request, user);
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
    public Long updatePost(Long postId, PostUpdateRequest request, Users user) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostException(PostErrorCode.NOT_FOUND));

        if (!post.getUser().getId().equals(user.getId())) {
            throw new PostException(PostErrorCode.FORBIDDEN);
        }

        // 기존 이미지 S3에서 삭제
        post.getImages().forEach(image -> s3Service.deleteImage(image.getImageUrl()));
        post.getImages().clear();

        // 새로운 이미지 업로드 및 PostImage 생성
        List<PostImage> postImages = createPostImages(request.images(), post);
        postImages.forEach(post::addImage);

        post.update(request.title(), request.content(), request.category(), postImages);
        return post.getId();
    }

    @Override
    @Transactional
    public void deletePost(Long postId, Users user) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostException(PostErrorCode.NOT_FOUND));

        if (!post.getUser().getId().equals(user.getId())) {
            throw new PostException(PostErrorCode.FORBIDDEN);
        }

        // 게시글 연관 이미지 S3에서도 삭제
        post.getImages().forEach(image -> s3Service.deleteImage(image.getImageUrl()));
        post.getImages().clear();

        // Soft Delete 처리
        post.delete();
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
}
