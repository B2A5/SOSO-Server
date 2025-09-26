package com.example.soso.community.common.post.service;

import com.example.soso.global.exception.domain.UserErrorCode;
import com.example.soso.global.exception.util.UserAuthException;
import com.example.soso.global.s3.GcsService;
import com.example.soso.global.s3.S3Service;
import com.example.soso.global.exception.domain.PostErrorCode;
import com.example.soso.global.exception.util.PostException;
import com.example.soso.community.common.likes.repository.PostLikeRepository;
import com.example.soso.community.common.post.domain.dto.CursorDto;
import com.example.soso.community.common.post.domain.dto.PostCreateRequest;
import com.example.soso.community.common.post.domain.dto.PostCreateResponse;
import com.example.soso.community.common.post.domain.dto.PostCursorResponse;
import com.example.soso.community.common.post.domain.dto.PostMapper;
import com.example.soso.community.common.post.domain.dto.PostResponse;
import com.example.soso.community.common.post.domain.dto.PostSortType;
import com.example.soso.community.common.post.domain.dto.PostSummaryResponse;
import com.example.soso.community.common.post.domain.dto.PostUpdateRequest;
import com.example.soso.community.common.post.domain.entity.Category;
import com.example.soso.community.common.post.domain.entity.Post;
import com.example.soso.community.common.post.domain.entity.PostImage;
import com.example.soso.community.common.post.repository.PostImageRepository;
import com.example.soso.community.common.post.repository.PostRepository;
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
// 컨트롤러(웹 요청 처리부)가 호출해서 “게시글 생성/조회/수정/삭제/목록” 같은 비즈니스 로직을 처리
public class PostServiceImpl implements PostService {
    // 의존성 주입을 통해 필요한 레포지토리와 서비스들을 주입받음
    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    // 이미지 업로드 및 삭제를 위한 서비스
    //private final GcsService gcsService;
    private final S3Service s3Service;
    private final UsersRepository usersRepository;
    private final PostLikeRepository postLikeRepository;

    // 게시글 생성 메서드
    // userId를 통해 작성자를 조회하고, 게시글 엔티티를 생성하여 저장
    @Override
    @Transactional
    public PostCreateResponse createPost(PostCreateRequest request, String userId) {
        Users users = getUserById(userId);

        Post post = PostMapper.toEntity(request, users);
        postRepository.save(post);

        List<PostImage> postImages = createPostImages(request.images(), post);
        postImages.forEach(image -> {
            postImageRepository.save(image);
            post.addImage(image);
        });

        return new PostCreateResponse(post.getId());
    }

    // 게시글 조회 메서드
    // postId로 게시글을 조회하고, 해당 게시글에 좋아요가 눌렸는지 여부를 확인
    @Override
    @Transactional(readOnly = true)
    public PostResponse getPost(Long postId, String userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostException(PostErrorCode.NOT_FOUND));
        boolean isLiked = postLikeRepository.existsByPost_IdAndUser_Id(postId, userId);

        return PostMapper.toResponse(post, isLiked);
    }

    // 게시글 목록 조회 메서드
    // 카테고리와 정렬 기준에 따라 커서 기반으로 게시글 목록을 조회
    @Override
    @Transactional(readOnly = true)
    public PostCursorResponse getPostsByCursor(Category category, PostSortType sort,
                                               int size, String cursor, Long idAfter, String userId) {

        // 1개 더 가져와서 hasNext 판단
        List<PostSummaryResponse> posts = postRepository.findAllByCursorPaging(category, sort, size + 1, cursor, idAfter, userId);

        boolean hasNext = posts.size() > size;

        String nextCursor = null;
        Long nextIdAfter = null;

        if (hasNext) {
            /** 커서 기준이 될 마지막 게시물 (제거 전에 보관)
             */
            PostSummaryResponse lastPost = posts.get(size);
            nextCursor = switch (sort) {
                case LATEST -> lastPost.createdAt().toString();
                case LIKE -> String.valueOf(lastPost.likeCount());
                case COMMENT -> String.valueOf(lastPost.commentCount());
                case VIEW -> String.valueOf(lastPost.viewCount());
            };
            nextIdAfter = lastPost.postId();

            // 응답에서 제외
            posts.remove(size);
        }

        CursorDto cursorDto = new CursorDto(hasNext, nextCursor, nextIdAfter);
        return new PostCursorResponse(posts, cursorDto);
    }

    // 게시글 수정 메서드
    // postId로 게시글을 조회하고, 작성자(userId)가 일치하는지 확인 후 수정
    @Override
    @Transactional
    public PostCreateResponse updatePost(Long postId, PostUpdateRequest request, String userId) {
        Post post = postRepository.findByIdAndUserId(postId, userId)
                .orElseThrow(() -> new PostException(PostErrorCode.FORBIDDEN));

        post.getImages().forEach(image -> s3Service.deleteImage(image.getImageUrl()));
        post.getImages().clear();

        List<PostImage> postImages = createPostImages(request.images(), post);
        postImages.forEach(post::addImage);

        post.update(request.title(), request.content(), request.category(), postImages);
        return new PostCreateResponse(post.getId());
    }
    // 게시글 삭제 메서드
    // postId로 게시글을 조회하고, 작성자(userId)가 일치하는지 확인 후 소프트 삭제
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
    // 게시글을 실제로 DB에서 삭제하는 메서드
    // 이 메서드는 소프트 삭제가 아닌 하드 삭제로, 이미지도 S3에서 삭제
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
     * 이미지 리스트가 null 또는 비어 있을 경우 안전하게 처리
     * 이미지 업로드 후 PostImage 엔티티 리스트를 생성
     */
    private List<PostImage> createPostImages(List<MultipartFile> images, Post post) {
        List<PostImage> postImages = new ArrayList<>();

        if (images == null || images.isEmpty()) {
            return postImages;
        }

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

    private Users getUserById(String userId) {
        return usersRepository.findById(userId)
                .orElseThrow(() -> new UserAuthException(UserErrorCode.USER_NOT_FOUND));
    }
}
