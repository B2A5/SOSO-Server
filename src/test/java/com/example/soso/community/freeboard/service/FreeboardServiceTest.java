package com.example.soso.community.freeboard.service;

import com.example.soso.community.freeboard.domain.dto.*;
import com.example.soso.global.exception.util.PostException;
import com.example.soso.global.exception.util.UserAuthException;
import com.example.soso.global.image.service.ImageUploadService;
import com.example.soso.likes.repository.PostLikeRepository;
import com.example.soso.post.domain.entity.Category;
import com.example.soso.post.domain.entity.Post;
import com.example.soso.post.domain.entity.PostImage;
import com.example.soso.post.repository.PostImageRepository;
import com.example.soso.post.repository.PostRepository;
import com.example.soso.users.domain.entity.Users;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
                .id("testUser123")
                .nickname("테스터")
                .email("test@example.com")
                .profileImageUrl("https://example.com/profile.jpg")
                .build();

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
                .id(1L)
                .imageUrl("https://example.com/image1.jpg")
                .post(testPost)
                .build();
        testPost.addImage(postImage);

        when(postRepository.findById(postId)).thenReturn(Optional.of(testPost));
        when(usersRepository.findById("testUser123")).thenReturn(Optional.of(testUser));
        when(postLikeRepository.existsByPostIdAndUserId(postId, "testUser123")).thenReturn(true);

        // when
        FreeboardDetailResponse result = freeboardService.getPost(postId, "testUser123");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getPostId()).isEqualTo(postId);
        assertThat(result.getTitle()).isEqualTo("맛있는 라면집 추천");
        assertThat(result.getCategory()).isEqualTo(Category.RESTAURANT);
        assertThat(result.getImageUrls()).hasSize(1);
        assertThat(result.getImageUrls().get(0)).isEqualTo("https://example.com/image1.jpg");
        assertThat(result.isLiked()).isTrue();
        assertThat(result.isAuthor()).isTrue(); // 같은 사용자
        assertThat(result.getLikeCount()).isEqualTo(15);
        assertThat(result.getCommentCount()).isEqualTo(8);

        verify(postRepository).findById(postId);
        verify(usersRepository).findById("testUser123");
        verify(postLikeRepository).existsByPostIdAndUserId(postId, "testUser123");
    }

    @Test
    @DisplayName("게시글 조회 실패 - 게시글 없음")
    void getPost_PostNotFound_ShouldThrowException() {
        // given
        Long postId = 999L;
        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> freeboardService.getPost(postId, "testUser123"))
                .isInstanceOf(PostException.class);

        verify(postRepository).findById(postId);
        verifyNoInteractions(usersRepository);
    }

    @ParameterizedTest
    @EnumSource(Category.class)
    @DisplayName("카테고리별 게시글 목록 조회")
    void getPostsByCursor_ByCategory(Category category) {
        // given
        List<Post> mockPosts = List.of(testPost);
        Page<Post> mockPage = new PageImpl<>(mockPosts);

        when(postRepository.findByCategoryAndDeletedFalse(eq(category), any(Pageable.class)))
                .thenReturn(mockPage);
        when(postLikeRepository.findPostIdsByPostIdsAndUserId(anyList(), eq("testUser123")))
                .thenReturn(Set.of());

        // when
        FreeboardCursorResponse result = freeboardService.getPostsByCursor(
                category, FreeboardSortType.LATEST, 10, null, "testUser123");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getPosts()).hasSize(1);
        assertThat(result.getPosts().get(0).getCategory()).isEqualTo(category);
        assertThat(result.isHasNext()).isFalse();

        verify(postRepository).findByCategoryAndDeletedFalse(eq(category), any(Pageable.class));
    }

    @Test
    @DisplayName("전체 카테고리 게시글 목록 조회")
    void getPostsByCursor_AllCategories() {
        // given
        List<Post> mockPosts = List.of(testPost);
        Page<Post> mockPage = new PageImpl<>(mockPosts);

        when(postRepository.findByDeletedFalse(any(Pageable.class)))
                .thenReturn(mockPage);
        when(postLikeRepository.findPostIdsByPostIdsAndUserId(anyList(), eq("testUser123")))
                .thenReturn(Set.of(123L));

        // when
        FreeboardCursorResponse result = freeboardService.getPostsByCursor(
                null, FreeboardSortType.LATEST, 10, null, "testUser123");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getPosts()).hasSize(1);
        assertThat(result.getPosts().get(0).isLiked()).isTrue(); // 좋아요 한 게시글
        assertThat(result.isHasNext()).isFalse();

        verify(postRepository).findByDeletedFalse(any(Pageable.class));
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

        when(postRepository.findById(postId)).thenReturn(Optional.of(testPost));

        // when
        FreeboardCreateResponse result = freeboardService.updatePost(postId, updateRequest, "testUser123");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getPostId()).isEqualTo(postId);

        verify(postRepository).findById(postId);
    }

    @Test
    @DisplayName("게시글 수정 실패 - 권한 없음")
    void updatePost_AccessDenied_ShouldThrowException() {
        // given
        Long postId = 123L;
        FreeboardUpdateRequest updateRequest = new FreeboardUpdateRequest();
        updateRequest.setTitle("수정된 제목");

        when(postRepository.findById(postId)).thenReturn(Optional.of(testPost));

        // when & then - 다른 사용자가 수정 시도
        assertThatThrownBy(() -> freeboardService.updatePost(postId, updateRequest, "otherUser"))
                .isInstanceOf(PostException.class);

        verify(postRepository).findById(postId);
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

        when(postRepository.findById(postId)).thenReturn(Optional.of(testPost));
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
        when(postRepository.findById(postId)).thenReturn(Optional.of(testPost));

        // when
        assertThatCode(() -> freeboardService.deletePost(postId, "testUser123"))
                .doesNotThrowAnyException();

        // then
        verify(postRepository).findById(postId);
    }

    @Test
    @DisplayName("게시글 영구 삭제 성공")
    void hardDeletePost_Success() {
        // given
        Long postId = 123L;
        PostImage postImage = PostImage.builder()
                .id(1L)
                .imageUrl("https://example.com/image1.jpg")
                .post(testPost)
                .build();
        testPost.addImage(postImage);

        when(postRepository.findById(postId)).thenReturn(Optional.of(testPost));

        // when
        assertThatCode(() -> freeboardService.hardDeletePost(postId, "testUser123"))
                .doesNotThrowAnyException();

        // then
        verify(postRepository).findById(postId);
        verify(imageUploadService).deleteImages(anyList());
        verify(postRepository).delete(testPost);
    }

    @Test
    @DisplayName("페이지 크기 제한 테스트")
    void getPostsByCursor_PageSizeLimit() {
        // given
        when(postRepository.findByDeletedFalse(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(postLikeRepository.findPostIdsByPostIdsAndUserId(anyList(), eq("testUser123")))
                .thenReturn(Set.of());

        // when & then - 최대 크기 초과
        FreeboardCursorResponse result1 = freeboardService.getPostsByCursor(
                null, FreeboardSortType.LATEST, 100, null, "testUser123"); // 50으로 제한됨

        assertThat(result1).isNotNull();

        // 음수 크기
        FreeboardCursorResponse result2 = freeboardService.getPostsByCursor(
                null, FreeboardSortType.LATEST, -5, null, "testUser123"); // 10으로 기본값

        assertThat(result2).isNotNull();
    }

    @Test
    @DisplayName("정렬 기준별 테스트")
    void getPostsByCursor_DifferentSortTypes() {
        // given
        when(postRepository.findByDeletedFalse(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(postLikeRepository.findPostIdsByPostIdsAndUserId(anyList(), eq("testUser123")))
                .thenReturn(Set.of());

        // when & then
        for (FreeboardSortType sortType : FreeboardSortType.values()) {
            FreeboardCursorResponse result = freeboardService.getPostsByCursor(
                    null, sortType, 10, null, "testUser123");

            assertThat(result).isNotNull();
        }

        verify(postRepository, times(4)).findByDeletedFalse(any(Pageable.class));
    }

    @Test
    @DisplayName("내용 미리보기 생성 테스트")
    void createContentPreview_Test() {
        // given
        Post longContentPost = Post.builder()
                .id(456L)
                .user(testUser)
                .category(Category.DAILY_HOBBY)
                .title("긴 내용 테스트")
                .content("a".repeat(150)) // 100자 초과 내용
                .likeCount(0)
                .commentCount(0)
                .images(new ArrayList<>())
                .build();

        when(postRepository.findByDeletedFalse(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(longContentPost)));
        when(postLikeRepository.findPostIdsByPostIdsAndUserId(anyList(), eq("testUser123")))
                .thenReturn(Set.of());

        // when
        FreeboardCursorResponse result = freeboardService.getPostsByCursor(
                null, FreeboardSortType.LATEST, 10, null, "testUser123");

        // then
        assertThat(result.getPosts()).hasSize(1);
        assertThat(result.getPosts().get(0).getContentPreview()).hasSize(103); // 100자 + "..."
        assertThat(result.getPosts().get(0).getContentPreview()).endsWith("...");
    }
}