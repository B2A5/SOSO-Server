package com.example.soso.community.freeboard.service;

import com.example.soso.community.freeboard.post.domain.dto.*;
import com.example.soso.community.freeboard.post.service.FreeboardServiceImpl;
import com.example.soso.global.exception.util.PostException;
import com.example.soso.global.exception.util.UserAuthException;
import com.example.soso.global.image.service.ImageUploadService;
import com.example.soso.community.freeboard.like.repository.PostLikeRepository;
import com.example.soso.community.common.post.domain.entity.Category;
import com.example.soso.community.freeboard.post.domain.entity.Post;
import com.example.soso.community.freeboard.post.domain.entity.PostImage;
import com.example.soso.community.freeboard.post.repository.PostImageRepository;
import com.example.soso.community.freeboard.post.repository.PostRepository;
import com.example.soso.community.freeboard.post.domain.dto.PostSortType;
import com.example.soso.community.freeboard.post.domain.dto.PostSummaryResponse;
import com.example.soso.community.common.post.domain.dto.UserSummaryResponse;
import com.example.soso.users.domain.entity.Users;
import com.example.soso.users.domain.entity.UserType;
import com.example.soso.users.repository.UsersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("자유게시판 서비스 테스트")
class FreeboardServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostImageRepository postImageRepository;

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private ImageUploadService imageUploadService;

    @InjectMocks
    private FreeboardServiceImpl freeboardService;

    private Users testUser;
    private Post testPost;
    private FreeboardCreateRequest testCreateRequest;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser = Users.builder()
                .nickname("테스터")
                .email("test@example.com")
                .profileImageUrl("https://example.com/profile.jpg")
                .build();
        // 리플렉션을 사용하여 id 설정
        try {
            java.lang.reflect.Field idField = Users.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(testUser, "testUser123");
        } catch (Exception e) {
            throw new RuntimeException("Failed to set user ID", e);
        }

        // 테스트 게시글 생성
        testPost = Post.builder()
                .id(123L)
                .user(testUser)
                .category(Category.RESTAURANT)
                .title("맛있는 라면집 추천")
                .content("어제 갔던 라면집이 정말 맛있었어요!")
                .likeCount(15)
                .commentCount(8)
                .images(new ArrayList<>())
                .build();

        // 테스트 요청 생성
        testCreateRequest = new FreeboardCreateRequest();
        testCreateRequest.setCategory(Category.RESTAURANT);
        testCreateRequest.setTitle("맛있는 라면집 추천");
        testCreateRequest.setContent("어제 갔던 라면집이 정말 맛있었어요!");

        MockMultipartFile mockImage = new MockMultipartFile(
                "images",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );
        testCreateRequest.setImages(List.of(mockImage));
    }

    @Test
    @DisplayName("게시글 작성 성공")
    void createPost_Success() {
        // given
        when(usersRepository.findById("testUser123")).thenReturn(Optional.of(testUser));
        when(postRepository.save(any(Post.class))).thenReturn(testPost);
        when(imageUploadService.uploadImages(anyList(), eq("freeboard")))
                .thenReturn(List.of("https://example.com/image1.jpg"));

        // when
        FreeboardCreateResponse result = freeboardService.createPost(testCreateRequest, "testUser123");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getPostId()).isEqualTo(123L);

        verify(usersRepository).findById("testUser123");
        verify(postRepository).save(any(Post.class));
        verify(imageUploadService).uploadImages(anyList(), eq("freeboard"));
    }

    @Test
    @DisplayName("게시글 작성 실패 - 사용자 없음")
    void createPost_UserNotFound_ShouldThrowException() {
        // given
        when(usersRepository.findById("invalidUser")).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> freeboardService.createPost(testCreateRequest, "invalidUser"))
                .isInstanceOf(UserAuthException.class);

        verify(usersRepository).findById("invalidUser");
        verifyNoInteractions(postRepository);
    }

    @Test
    @DisplayName("게시글 작성 성공 - 이미지 없음")
    void createPost_WithoutImages_Success() {
        // given
        testCreateRequest.setImages(null);
        when(usersRepository.findById("testUser123")).thenReturn(Optional.of(testUser));
        when(postRepository.save(any(Post.class))).thenReturn(testPost);

        // when
        FreeboardCreateResponse result = freeboardService.createPost(testCreateRequest, "testUser123");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getPostId()).isEqualTo(123L);

        verify(usersRepository).findById("testUser123");
        verify(postRepository).save(any(Post.class));
        verifyNoInteractions(imageUploadService);
    }

    @Test
    @DisplayName("게시글 조회 성공")
    void getPost_Success() {
        // given
        Long postId = 123L;
        PostImage postImage = PostImage.builder()
                .imageUrl("https://example.com/image1.jpg")
                .sequence(0)
                .post(testPost)
                .build();
        // PostImage에 ID 설정 (리플렉션 사용)
        try {
            java.lang.reflect.Field idField = PostImage.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(postImage, 1L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        testPost.addImage(postImage);

        when(postRepository.findByIdAndDeletedFalse(postId)).thenReturn(Optional.of(testPost));
        // getPost 메서드에서 findUserById를 호출하지 않으므로 mocking 제거
        // when(usersRepository.findById("testUser123")).thenReturn(Optional.of(testUser));
        when(postLikeRepository.existsByPost_IdAndUser_Id(postId, "testUser123")).thenReturn(true);

        // when
        FreeboardDetailResponse result = freeboardService.getPost(postId, "testUser123");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getPostId()).isEqualTo(postId);
        assertThat(result.getTitle()).isEqualTo("맛있는 라면집 추천");
        assertThat(result.getCategory()).isEqualTo(Category.RESTAURANT);
        assertThat(result.getImages()).hasSize(1);
        assertThat(result.getImages().get(0).getImageId()).isEqualTo(1L);
        assertThat(result.getImages().get(0).getImageUrl()).isEqualTo("https://example.com/image1.jpg");
        assertThat(result.getImages().get(0).getSequence()).isEqualTo(0);
        assertThat(result.isLiked()).isTrue();
        // assertThat(result.isAuthor()).isTrue(); // 같은 사용자 - TODO: Check method generation
        assertThat(result.getLikeCount()).isEqualTo(15);
        assertThat(result.getCommentCount()).isEqualTo(8);
        assertThat(result.getViewCount()).isEqualTo(1);

        verify(postRepository).findByIdAndDeletedFalse(postId);
        // userId가 null이 아니더라도 getPost 메서드에서 findUserById를 호출하지 않음
        // verify(usersRepository).findById("testUser123"); // 제거
        verify(postLikeRepository).existsByPost_IdAndUser_Id(postId, "testUser123");
    }

    @Test
    @DisplayName("게시글 조회 실패 - 게시글 없음")
    void getPost_PostNotFound_ShouldThrowException() {
        // given
        Long postId = 999L;
        when(postRepository.findByIdAndDeletedFalse(postId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> freeboardService.getPost(postId, "testUser123"))
                .isInstanceOf(PostException.class);

        verify(postRepository).findByIdAndDeletedFalse(postId);
        verifyNoInteractions(usersRepository);
    }

    @ParameterizedTest
    @EnumSource(Category.class)
    @DisplayName("카테고리별 게시글 목록 조회")
    void getPostsByCursor_ByCategory(Category category) {
        // given
        UserSummaryResponse mockUser = new UserSummaryResponse(
                "testUser123", "테스터", "서울시 강남구",
                "https://example.com/profile.jpg", UserType.INHABITANT
        );

        PostSummaryResponse mockPostSummary = new PostSummaryResponse(
                1L, "테스트 제목", "테스트 내용", category,
                15, 8, 120, false,
                LocalDateTime.of(2024, 12, 25, 10, 30, 0), // createdAt
                LocalDateTime.of(2024, 12, 25, 10, 30, 0), // updatedAt
                null, // thumbnailUrl
                0,    // imageCount
                mockUser
        );

        List<PostSummaryResponse> mockPostSummaries = List.of(mockPostSummary);

        when(postRepository.findAllByCursorPaging(
                eq(category), eq(PostSortType.LATEST), eq(11), eq(null), eq(null), eq("testUser123")))
                .thenReturn(mockPostSummaries);

        // when
        FreeboardCursorResponse result = freeboardService.getPostsByCursor(
                category, FreeboardSortType.LATEST, 10, null, "testUser123");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getPosts()).hasSize(1);
        assertThat(result.getPosts().get(0).getCategory()).isEqualTo(category);
        assertThat(result.getPosts().get(0).getThumbnailUrl()).isNull();
        assertThat(result.getPosts().get(0).getImageCount()).isEqualTo(0);
        assertThat(result.isHasNext()).isFalse();

        verify(postRepository).findAllByCursorPaging(
                eq(category), eq(PostSortType.LATEST), eq(11), eq(null), eq(null), eq("testUser123"));
    }

    @Test
    @DisplayName("전체 카테고리 게시글 목록 조회")
    void getPostsByCursor_AllCategories() {
        // given
        UserSummaryResponse mockUser = new UserSummaryResponse(
                "testUser123", "테스터", "서울시 강남구",
                "https://example.com/profile.jpg", UserType.INHABITANT
        );

        PostSummaryResponse mockPostSummary = new PostSummaryResponse(
                123L, "테스트 제목", "테스트 내용", Category.RESTAURANT,
                10, 5, 77, true,
                LocalDateTime.of(2024, 12, 25, 10, 30, 0), // createdAt
                LocalDateTime.of(2024, 12, 25, 11, 0, 0),  // updatedAt
                "https://example.com/thumb.jpg", // thumbnailUrl
                2,    // imageCount
                mockUser
        );

        List<PostSummaryResponse> mockPostSummaries = List.of(mockPostSummary);

        when(postRepository.findAllByCursorPaging(
                eq(null), eq(PostSortType.LATEST), eq(11), eq(null), eq(null), eq("testUser123")))
                .thenReturn(mockPostSummaries);

        // when
        FreeboardCursorResponse result = freeboardService.getPostsByCursor(
                null, FreeboardSortType.LATEST, 10, null, "testUser123");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getPosts()).hasSize(1);
        assertThat(result.getPosts().get(0).isLiked()).isTrue(); // 좋아요 한 게시글
        assertThat(result.getPosts().get(0).getThumbnailUrl()).isEqualTo("https://example.com/thumb.jpg");
        assertThat(result.getPosts().get(0).getImageCount()).isEqualTo(2);
        assertThat(result.getPosts().get(0).getUpdatedAt()).isNotNull();
        assertThat(result.isHasNext()).isFalse();

        verify(postRepository).findAllByCursorPaging(
                eq(null), eq(PostSortType.LATEST), eq(11), eq(null), eq(null), eq("testUser123"));
    }

    @Test
    @DisplayName("게시글 수정 성공")
    void updatePost_Success() {
        // given
        Long postId = 123L;
        FreeboardUpdateRequest updateRequest = new FreeboardUpdateRequest();
        updateRequest.setTitle("수정된 제목");
        updateRequest.setContent("수정된 내용");
        updateRequest.setCategory(Category.LIVING_CONVENIENCE);

        when(postRepository.findByIdAndDeletedFalse(postId)).thenReturn(Optional.of(testPost));

        // when
        FreeboardCreateResponse result = freeboardService.updatePost(postId, updateRequest, "testUser123");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getPostId()).isEqualTo(postId);

        verify(postRepository).findByIdAndDeletedFalse(postId);
    }

    @Test
    @DisplayName("게시글 수정 실패 - 권한 없음")
    void updatePost_AccessDenied_ShouldThrowException() {
        // given
        Long postId = 123L;
        FreeboardUpdateRequest updateRequest = new FreeboardUpdateRequest();
        updateRequest.setTitle("수정된 제목");

        when(postRepository.findByIdAndDeletedFalse(postId)).thenReturn(Optional.of(testPost));

        // when & then - 다른 사용자가 수정 시도
        assertThatThrownBy(() -> freeboardService.updatePost(postId, updateRequest, "otherUser"))
                .isInstanceOf(PostException.class);

        verify(postRepository).findByIdAndDeletedFalse(postId);
    }

    @Test
    @DisplayName("게시글 수정 - 새 이미지 추가")
    void updatePost_WithNewImages_Success() {
        // given
        Long postId = 123L;
        FreeboardUpdateRequest updateRequest = new FreeboardUpdateRequest();
        updateRequest.setTitle("수정된 제목");

        MockMultipartFile newImage = new MockMultipartFile(
                "images", "new.jpg", "image/jpeg", "new image content".getBytes()
        );
        updateRequest.setImages(List.of(newImage));

        when(postRepository.findByIdAndDeletedFalse(postId)).thenReturn(Optional.of(testPost));
        when(imageUploadService.uploadImages(anyList(), eq("freeboard")))
                .thenReturn(List.of("https://example.com/new_image.jpg"));
        when(imageUploadService.getMaxImageCount()).thenReturn(4);

        // when
        FreeboardCreateResponse result = freeboardService.updatePost(postId, updateRequest, "testUser123");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getPostId()).isEqualTo(postId);

        verify(imageUploadService).uploadImages(anyList(), eq("freeboard"));
    }

    @Test
    @DisplayName("게시글 삭제 성공")
    void deletePost_Success() {
        // given
        Long postId = 123L;
        when(postRepository.findByIdAndDeletedFalse(postId)).thenReturn(Optional.of(testPost));

        // when
        assertThatCode(() -> freeboardService.deletePost(postId, "testUser123"))
                .doesNotThrowAnyException();

        // then
        verify(postRepository).findByIdAndDeletedFalse(postId);
    }

    @Test
    @DisplayName("게시글 영구 삭제 성공")
    void hardDeletePost_Success() {
        // given
        Long postId = 123L;
        PostImage postImage = PostImage.builder()
                .imageUrl("https://example.com/image1.jpg")
                .post(testPost)
                .build();
        testPost.addImage(postImage);

        when(postRepository.findByIdAndDeletedFalse(postId)).thenReturn(Optional.of(testPost));

        // when
        assertThatCode(() -> freeboardService.hardDeletePost(postId, "testUser123"))
                .doesNotThrowAnyException();

        // then
        verify(postRepository).findByIdAndDeletedFalse(postId);
        verify(imageUploadService).deleteImages(anyList());
        verify(postRepository).delete(testPost);
    }

    @Test
    @DisplayName("페이지 크기 제한 테스트")
    void getPostsByCursor_PageSizeLimit() {
        // given
        when(postRepository.findAllByCursorPaging(any(), any(), anyInt(), any(), any(), anyString()))
                .thenReturn(List.of());

        // when & then - 최대 크기 초과
        FreeboardCursorResponse result1 = freeboardService.getPostsByCursor(
                null, FreeboardSortType.LATEST, 100, null, "testUser123"); // 50으로 제한됨

        assertThat(result1).isNotNull();
        verify(postRepository).findAllByCursorPaging(
                eq(null), eq(PostSortType.LATEST), eq(51), eq(null), eq(null), eq("testUser123")); // 50 + 1

        // 음수 크기
        FreeboardCursorResponse result2 = freeboardService.getPostsByCursor(
                null, FreeboardSortType.LATEST, -5, null, "testUser123"); // 10으로 기본값

        assertThat(result2).isNotNull();
        verify(postRepository).findAllByCursorPaging(
                eq(null), eq(PostSortType.LATEST), eq(11), eq(null), eq(null), eq("testUser123")); // 10 + 1
    }

    @Test
    @DisplayName("정렬 기준별 테스트")
    void getPostsByCursor_DifferentSortTypes() {
        // given
        when(postRepository.findAllByCursorPaging(any(), any(), anyInt(), any(), any(), anyString()))
                .thenReturn(List.of());

        // when & then
        for (FreeboardSortType sortType : FreeboardSortType.values()) {
            FreeboardCursorResponse result = freeboardService.getPostsByCursor(
                    null, sortType, 10, null, "testUser123");

            assertThat(result).isNotNull();
        }

        // 총 4번 호출되었는지 확인 (FreeboardSortType.values() 개수)
        verify(postRepository, times(4)).findAllByCursorPaging(any(), any(), anyInt(), any(), any(), anyString());
    }

    @Test
    @DisplayName("내용 미리보기 생성 테스트")
    void createContentPreview_Test() {
        // given
        String longContent = "a".repeat(150); // 100자 초과 내용
        UserSummaryResponse mockUser = new UserSummaryResponse(
                "testUser123", "테스터", "서울시 강남구",
                "https://example.com/profile.jpg", UserType.INHABITANT
        );

        PostSummaryResponse mockPostSummary = new PostSummaryResponse(
                456L, "긴 내용 테스트", longContent, Category.DAILY_HOBBY,
                0, 0, 5, false,
                LocalDateTime.of(2024, 12, 25, 10, 30, 0), // createdAt
                LocalDateTime.of(2024, 12, 25, 10, 30, 0), // updatedAt
                null, // thumbnailUrl
                0,    // imageCount
                mockUser
        );

        when(postRepository.findAllByCursorPaging(
                eq(null), eq(PostSortType.LATEST), eq(11), eq(null), eq(null), eq("testUser123")))
                .thenReturn(List.of(mockPostSummary));

        // when
        FreeboardCursorResponse result = freeboardService.getPostsByCursor(
                null, FreeboardSortType.LATEST, 10, null, "testUser123");

        // then
        assertThat(result.getPosts()).hasSize(1);
        assertThat(result.getPosts().get(0).getContentPreview()).hasSize(103); // 100자 + "..."
        assertThat(result.getPosts().get(0).getContentPreview()).endsWith("...");
    }
}
