package com.example.soso.post.service;

import com.example.soso.global.config.S3Service;
import com.example.soso.global.exception.domain.PostErrorCode;
import com.example.soso.global.exception.util.PostException;
import com.example.soso.post.domain.dto.PostCreateRequest;
import com.example.soso.post.domain.dto.PostMapper;
import com.example.soso.post.domain.dto.PostResponse;
import com.example.soso.post.domain.entity.Post;
import com.example.soso.post.domain.entity.PostImage;
import com.example.soso.post.repository.PostImageRepository;
import com.example.soso.post.repository.PostRepository;
import com.example.soso.users.domain.entity.Users;
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
        // 1. 게시글 본문 저장
        Post post = PostMapper.toEntity(request, user);
        postRepository.save(post);

        // 2. 이미지 업로드 + PostImage 저장
        for (int i = 0; i < request.images().size(); i++) {
            MultipartFile imageFile = request.images().get(i);

            String imageUrl = s3Service.uploadImage(imageFile, "posts");

            PostImage postImage = PostImage.builder()
                    .imageUrl(imageUrl)
                    .sequence(i)
                    .post(post)
                    .build();

            postImageRepository.save(postImage);
            post.addImage(postImage); // 연관관계 편의 메서드
        }
        return post.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public PostResponse getPost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostException(PostErrorCode.NOT_FOUND));

        return PostMapper.toResponse(post);
    }


}
