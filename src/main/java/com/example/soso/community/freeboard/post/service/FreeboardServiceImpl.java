package com.example.soso.community.freeboard.post.service;

import com.example.soso.community.freeboard.post.domain.dto.*;
import com.example.soso.global.exception.domain.PostErrorCode;
import com.example.soso.global.exception.domain.UserErrorCode;
import com.example.soso.global.exception.util.PostException;
import com.example.soso.global.exception.util.UserAuthException;
import com.example.soso.global.image.service.ImageUploadService;
import com.example.soso.community.common.likes.repository.PostLikeRepository;
import com.example.soso.community.common.post.domain.entity.Category;
import com.example.soso.community.common.post.domain.entity.Post;
import com.example.soso.community.common.post.domain.entity.PostImage;
import com.example.soso.community.common.post.repository.PostImageRepository;
import com.example.soso.community.common.post.repository.PostRepository;
import com.example.soso.users.domain.entity.Users;
import com.example.soso.users.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.example.soso.community.common.post.domain.dto.PostSortType;
import com.example.soso.community.common.post.domain.dto.PostSummaryResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 자유게시판 비즈니스 로직 구현체
 *
 * 주요 책임:
 * - 게시글 CRUD 작업
 * - 이미지 업로드/삭제 관리
 * - 커서 기반 페이지네이션
 * - 권한 검증 및 데이터 검증
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FreeboardServiceImpl implements FreeboardService {

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final UsersRepository usersRepository;
    private final PostLikeRepository postLikeRepository;
    private final ImageUploadService imageUploadService;

    private static final String FREEBOARD_DIRECTORY = "freeboard";
    private static final int CONTENT_PREVIEW_LENGTH = 100;

    @Override
    @Transactional
    public FreeboardCreateResponse createPost(FreeboardCreateRequest request, String userId) {
        log.info("자유게시판 글 작성 시작: userId={}, category={}", userId, request.getCategory());

        // 사용자 조회
        Users user = findUserById(userId);

        // 게시글 엔티티 생성
        Post post = Post.builder()
                .user(user)
                .category(request.getCategory())
                .title(request.getTitle())
                .content(request.getContent())
                .likeCount(0)
                .commentCount(0)
                .build();

        // 게시글 저장
        Post savedPost = postRepository.save(post);

        // 이미지 업로드 및 저장
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            List<String> imageUrls = imageUploadService.uploadImages(request.getImages(), FREEBOARD_DIRECTORY);
            savePostImages(savedPost, imageUrls);
        }

        log.info("자유게시판 글 작성 완료: postId={}, imageCount={}",
                savedPost.getId(),
                savedPost.getImages().size());

        return new FreeboardCreateResponse(savedPost.getId());
    }

    @Override
    public FreeboardDetailResponse getPost(Long postId, String userId) {
        log.debug("자유게시판 글 조회: postId={}, userId={}", postId, userId);

        // 게시글 조회 (삭제되지 않은 글만) - 비인증 사용자도 조회 가능
        Post post = postRepository.findByIdAndDeletedFalse(postId)
                .orElseThrow(() -> new PostException(PostErrorCode.POST_NOT_FOUND));

        // 좋아요 여부 확인 (인증된 사용자인 경우만)
        boolean isLiked = userId != null && postLikeRepository.existsByPost_IdAndUser_Id(postId, userId);

        // 이미지 URL 목록 추출
        List<String> imageUrls = post.getImages().stream()
                .map(PostImage::getImageUrl)
                .toList();

        // 작성자 여부 확인
        boolean isAuthor = userId != null && post.getUser().getId().equals(userId);

        // 응답 DTO 생성
        return FreeboardDetailResponse.builder()
                .postId(post.getId())
                .author(FreeboardDetailResponse.PostDetailAuthorInfo.builder()
                        .userId(post.getUser().getId())
                        .nickname(post.getUser().getNickname())
                        .profileImageUrl(post.getUser().getProfileImageUrl())
                        .userType(post.getUser().getUserType())
                        .address(post.getUser().getLocation())
                        .build())
                .category(post.getCategory())
                .title(post.getTitle())
                .content(post.getContent())
                .imageUrls(imageUrls)
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .viewCount(0) // TODO: 조회수 기능 구현 필요
                .isLiked(isLiked)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .isAuthor(isAuthor)
                .canEdit(userId != null && isAuthor) // 인증된 사용자이고 작성자인 경우만 편집 가능
                .canDelete(userId != null && isAuthor) // 인증된 사용자이고 작성자인 경우만 삭제 가능
                .build();
    }

    @Override
    public FreeboardCursorResponse getPostsByCursor(Category category, FreeboardSortType sort, int size, String cursor, String userId) {
        log.debug("자유게시판 목록 조회: category={}, sort={}, size={}, userId={}", category, sort, size, userId);

        // 페이지 크기 제한
        if (size > 50) size = 50;
        if (size < 1) size = 10;

        // PostRepositoryCustom의 findAllByCursorPaging 메서드 사용
        PostSortType postSortType = convertToPostSortType(sort);
        List<PostSummaryResponse> postSummaries = postRepository.findAllByCursorPaging(
            category, postSortType, size + 1, cursor, null, userId
        );

        // 총 게시글 수 조회
        long totalCount = category != null
            ? postRepository.countByCategoryAndDeletedFalse(category)
            : postRepository.countByDeletedFalse();

        // 다음 페이지 존재 여부 확인
        boolean hasNext = postSummaries.size() > size;
        if (hasNext) {
            postSummaries = postSummaries.subList(0, size);
        }

        // PostSummaryResponse를 FreeboardSummary로 변환
        List<FreeboardCursorResponse.FreeboardSummary> summaries = postSummaries.stream()
                .map(this::convertToFreeboardSummary)
                .toList();

        // 다음 커서 생성 (간단히 null로 처리, 실제로는 커서 생성 로직 필요)
        String nextCursor = hasNext ? "next" : null;

        return FreeboardCursorResponse.builder()
                .posts(summaries)
                .hasNext(hasNext)
                .nextCursor(nextCursor)
                .size(summaries.size())
                .totalCount(totalCount)
                .build();
    }

    @Override
    @Transactional
    public FreeboardCreateResponse updatePost(Long postId, FreeboardUpdateRequest request, String userId) {
        log.info("자유게시판 글 수정 시작: postId={}, userId={}", postId, userId);

        // 게시글 조회 및 권한 확인
        Post post = findPostByIdAndUserId(postId, userId);

        // 기존 이미지 삭제 처리
        if (request.getDeleteImageIds() != null && !request.getDeleteImageIds().isEmpty()) {
            deletePostImages(post, request.getDeleteImageIds());
        }

        // 새로운 이미지 업로드
        List<String> newImageUrls = Collections.emptyList();
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            // 현재 이미지 개수 + 새 이미지 개수가 4개를 초과하지 않는지 확인
            int currentImageCount = post.getImages().size();
            int newImageCount = request.getImages().size();
            if (currentImageCount + newImageCount > imageUploadService.getMaxImageCount()) {
                throw new IllegalArgumentException("총 이미지 개수는 " + imageUploadService.getMaxImageCount() + "개를 초과할 수 없습니다.");
            }

            newImageUrls = imageUploadService.uploadImages(request.getImages(), FREEBOARD_DIRECTORY);
            savePostImages(post, newImageUrls);
        }

        // 게시글 정보 업데이트
        updatePostFields(post, request);

        log.info("자유게시판 글 수정 완료: postId={}, newImageCount={}", postId, newImageUrls.size());

        return new FreeboardCreateResponse(postId);
    }

    @Override
    @Transactional
    public void deletePost(Long postId, String userId) {
        log.info("자유게시판 글 소프트 삭제: postId={}, userId={}", postId, userId);

        Post post = findPostByIdAndUserId(postId, userId);
        post.delete();

        log.info("자유게시판 글 소프트 삭제 완료: postId={}", postId);
    }

    @Override
    @Transactional
    public void hardDeletePost(Long postId, String userId) {
        log.warn("자유게시판 글 하드 삭제 시작: postId={}, userId={}", postId, userId);

        // TODO: 관리자 권한 확인 로직 추가
        Post post = findPostByIdAndUserId(postId, userId);

        // S3에서 이미지 삭제
        List<String> imageUrls = post.getImages().stream()
                .map(PostImage::getImageUrl)
                .toList();

        if (!imageUrls.isEmpty()) {
            imageUploadService.deleteImages(imageUrls);
        }

        // 데이터베이스에서 완전 삭제
        postRepository.delete(post);

        log.warn("자유게시판 글 하드 삭제 완료: postId={}, deletedImageCount={}", postId, imageUrls.size());
    }

    // === 내부 헬퍼 메서드들 ===

    private Users findUserById(String userId) {
        return usersRepository.findById(userId)
                .orElseThrow(() -> new UserAuthException(UserErrorCode.USER_NOT_FOUND));
    }

    private Post findPostByIdAndUserId(Long postId, String userId) {
        Post post = postRepository.findByIdAndDeletedFalse(postId)
                .orElseThrow(() -> new PostException(PostErrorCode.POST_NOT_FOUND));

        if (!post.getUser().getId().equals(userId)) {
            throw new PostException(PostErrorCode.POST_ACCESS_DENIED);
        }

        return post;
    }

    private void savePostImages(Post post, List<String> imageUrls) {
        for (String imageUrl : imageUrls) {
            PostImage postImage = PostImage.builder()
                    .post(post)
                    .imageUrl(imageUrl)
                    .build();
            post.addImage(postImage);
        }
    }

    private void deletePostImages(Post post, List<Long> deleteImageIds) {
        List<PostImage> imagesToDelete = post.getImages().stream()
                .filter(image -> deleteImageIds.contains(image.getId()))
                .toList();

        for (PostImage image : imagesToDelete) {
            imageUploadService.deleteImage(image.getImageUrl());
            post.getImages().remove(image);
        }
    }

    private void updatePostFields(Post post, FreeboardUpdateRequest request) {
        List<PostImage> newImages = new ArrayList<>(post.getImages());

        post.update(
                StringUtils.hasText(request.getTitle()) ? request.getTitle() : post.getTitle(),
                StringUtils.hasText(request.getContent()) ? request.getContent() : post.getContent(),
                request.getCategory() != null ? request.getCategory() : post.getCategory(),
                newImages
        );
    }

    private CursorInfo parseCursor(String cursor, FreeboardSortType sort) {
        if (!StringUtils.hasText(cursor)) {
            return new CursorInfo(null, null);
        }

        try {
            // Base64 디코딩 후 JSON 파싱 (간단 구현)
            String decodedCursor = new String(Base64.getDecoder().decode(cursor));
            // 실제로는 JSON 라이브러리 사용 권장
            // 여기서는 간단히 구현
            return new CursorInfo(null, null);
        } catch (Exception e) {
            log.warn("커서 파싱 실패: cursor={}, error={}", cursor, e.getMessage());
            return new CursorInfo(null, null);
        }
    }

    private PostSortType convertToPostSortType(FreeboardSortType freeboardSortType) {
        return switch (freeboardSortType) {
            case LATEST -> PostSortType.LATEST;
            case LIKE -> PostSortType.LIKE;
            case COMMENT -> PostSortType.COMMENT;
            case VIEW -> PostSortType.LATEST; // PostSortType에 VIEW가 없으므로 LATEST로 처리
        };
    }

    private FreeboardCursorResponse.FreeboardSummary convertToFreeboardSummary(PostSummaryResponse postSummary) {
        // UserSummaryResponse에서 AuthorInfo로 변환
        FreeboardCursorResponse.PostAuthorInfo authorInfo = null;
        if (postSummary.user() != null) {
            authorInfo = FreeboardCursorResponse.PostAuthorInfo.builder()
                    .userId(postSummary.user().userId())
                    .nickname(postSummary.user().nickname())
                    .profileImageUrl(postSummary.user().profileImageUrl())
                    .userType(postSummary.user().userType())
                    .build();
        }

        return FreeboardCursorResponse.FreeboardSummary.builder()
                .postId(postSummary.postId())
                .author(authorInfo)
                .category(postSummary.category())
                .title(postSummary.title())
                .contentPreview(postSummary.content().length() > 100 ?
                    postSummary.content().substring(0, 100) + "..." :
                    postSummary.content())
                .likeCount(postSummary.likeCount())
                .commentCount(postSummary.commentCount())
                .isLiked(postSummary.likeByPost())
                .createdAt(postSummary.createdAt())
                .viewCount(0) // TODO: viewCount 필드 추가 필요
                .thumbnailUrl(null) // TODO: thumbnailUrl 처리 필요
                .imageCount(0) // TODO: imageCount 처리 필요
                .updatedAt(null) // TODO: updatedAt 처리 필요
                .build();
    }

    private Sort createSort(FreeboardSortType sortType) {
        return switch (sortType) {
            case LATEST -> Sort.by(Sort.Direction.DESC, "createdAt");
            case LIKE -> Sort.by(Sort.Direction.DESC, "likeCount", "createdAt");
            case COMMENT -> Sort.by(Sort.Direction.DESC, "commentCount", "createdAt");
            case VIEW -> Sort.by(Sort.Direction.DESC, "createdAt"); // TODO: viewCount 필드 추가 후 수정
        };
    }

    private Set<Long> getLikedPostIds(List<Post> posts, String userId) {
        if (userId == null || posts.isEmpty()) {
            return Collections.emptySet();
        }
        List<Long> postIds = posts.stream().map(Post::getId).toList();
        return postLikeRepository.findPostIdsByPostIdsAndUserId(postIds, userId);
    }

    private FreeboardCursorResponse.FreeboardSummary createFreeboardSummary(Post post, boolean isLiked) {
        List<String> imageUrls = post.getImages().stream()
                .map(PostImage::getImageUrl)
                .toList();

        String contentPreview = createContentPreview(post.getContent());
        String thumbnailUrl = imageUrls.isEmpty() ? null : imageUrls.get(0);

        return FreeboardCursorResponse.FreeboardSummary.builder()
                .postId(post.getId())
                .author(FreeboardCursorResponse.PostAuthorInfo.builder()
                        .userId(post.getUser().getId())
                        .nickname(post.getUser().getNickname())
                        .profileImageUrl(post.getUser().getProfileImageUrl())
                        .build())
                .category(post.getCategory())
                .title(post.getTitle())
                .contentPreview(contentPreview)
                .thumbnailUrl(thumbnailUrl)
                .imageCount(imageUrls.size())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .viewCount(0) // TODO: 조회수 기능 구현
                .isLiked(isLiked)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    private String createContentPreview(String content) {
        if (content == null || content.length() <= CONTENT_PREVIEW_LENGTH) {
            return content;
        }
        return content.substring(0, CONTENT_PREVIEW_LENGTH) + "...";
    }

    private String generateCursor(Post post, FreeboardSortType sort) {
        // 실제 구현에서는 JSON으로 커서 정보 직렬화
        String cursorValue = switch (sort) {
            case LATEST -> post.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            case LIKE -> String.valueOf(post.getLikeCount());
            case COMMENT -> String.valueOf(post.getCommentCount());
            case VIEW -> post.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME); // TODO: viewCount 사용
        };

        String cursorData = String.format("{\"id\":%d,\"sortValue\":\"%s\"}", post.getId(), cursorValue);
        return Base64.getEncoder().encodeToString(cursorData.getBytes());
    }

    // 커서 정보를 담는 내부 클래스
    private record CursorInfo(Long lastId, String lastValue) {
    }

}