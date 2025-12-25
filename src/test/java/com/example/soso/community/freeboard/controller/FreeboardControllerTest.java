package com.example.soso.community.freeboard.controller;

import com.example.soso.community.freeboard.post.domain.dto.*;
import com.example.soso.community.freeboard.post.service.FreeboardService;
import com.example.soso.community.common.post.domain.entity.Category;
import com.example.soso.security.domain.CustomUserDetails;
import com.example.soso.users.domain.entity.Users;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.session.store-type=none",
        "jwt.secret-key=test-jwt-secret-key-that-is-sufficiently-long-and-secure-for-testing-purposes-minimum-256-bits-required-by-jwt-library",
        "jwt.access-token-validity-in-ms=1800000",
        "jwt.refresh-token-validity-in-ms=1209600000"
})
@Transactional
@DisplayName("자유게시판 컨트롤러 테스트")
class FreeboardControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private FreeboardService freeboardService;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private CustomUserDetails testUserDetails;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        // 테스트용 사용자 설정
        Users testUser = Users.builder()
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

        testUserDetails = new CustomUserDetails(testUser);
    }

    @Test
    @DisplayName("자유게시판 글 작성 성공")
    void createPost_Success() throws Exception {
        // given
        FreeboardCreateResponse mockResponse = new FreeboardCreateResponse(123L);
        when(freeboardService.createPost(any(FreeboardCreateRequest.class), eq("testUser123")))
                .thenReturn(mockResponse);

        MockMultipartFile imagePart = new MockMultipartFile("images", "test.jpg", "image/jpeg", "test image content".getBytes());

        // when & then
        mockMvc.perform(multipart("/community/freeboard")
                        .file(imagePart)
                        .param("title", "맛있는 라면집 추천")
                        .param("content", "어제 갔던 라면집이 정말 맛있었어요!")
                        .param("category", "RESTAURANT")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(123));
    }

    @Test
    @DisplayName("자유게시판 글 작성 - 빈 제목으로 실패")
    void createPost_EmptyTitle_ShouldFail() throws Exception {
        // when & then
        mockMvc.perform(multipart("/community/freeboard")
                        .param("title", "")
                        .param("content", "내용")
                        .param("category", "RESTAURANT")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("자유게시판 글 상세 조회 성공")
    void getPost_Success() throws Exception {
        // given
        Long postId = 123L;
        FreeboardDetailResponse mockResponse = FreeboardDetailResponse.builder()
                .postId(postId)
                .author(FreeboardDetailResponse.PostDetailAuthorInfo.builder()
                        .userId("author123")
                        .nickname("작성자")
                        .profileImageUrl("https://example.com/author.jpg")
                        .build())
                .category(Category.RESTAURANT)
                .title("맛있는 라면집 추천")
                .content("어제 갔던 라면집이 정말 맛있었어요!")
                .images(List.of(FreeboardDetailResponse.ImageInfo.builder()
                        .imageId(1L)
                        .imageUrl("https://example.com/image1.jpg")
                        .sequence(0)
                        .build()))
                .likeCount(15)
                .commentCount(8)
                .viewCount(102)
                .isLiked(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isAuthor(false)
                .build();

        when(freeboardService.getPost(eq(postId), eq("testUser123")))
                .thenReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/community/freeboard/{freeboardId}", postId)
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(postId))
                .andExpect(jsonPath("$.title").value("맛있는 라면집 추천"))
                .andExpect(jsonPath("$.category").value("restaurant"))
                .andExpect(jsonPath("$.author.nickname").value("작성자"))
                .andExpect(jsonPath("$.likeCount").value(15))
                .andExpect(jsonPath("$.commentCount").value(8));
    }

    @ParameterizedTest
    @EnumSource(Category.class)
    @DisplayName("자유게시판 목록 조회 - 모든 카테고리 테스트")
    void getPostsByCursor_AllCategories(Category category) throws Exception {
        // given
        FreeboardCursorResponse mockResponse = FreeboardCursorResponse.builder()
                .posts(List.of(
                        FreeboardCursorResponse.FreeboardSummary.builder()
                                .postId(1L)
                                .category(category)
                                .title("테스트 제목")
                                .contentPreview("테스트 내용 미리보기...")
                                .author(FreeboardCursorResponse.PostAuthorInfo.builder()
                                        .userId("author123")
                                        .nickname("작성자")
                                        .build())
                                .likeCount(5)
                                .commentCount(3)
                                .viewCount(50)
                                .imageCount(1)
                                .isLiked(false)
                                .createdAt(LocalDateTime.now())
                                .build()
                ))
                .hasNext(false)
                .nextCursor(null)
                .size(1)
                .build();

        when(freeboardService.getPostsByCursor(eq(category), any(), anyInt(), any(), eq("testUser123")))
                .thenReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/community/freeboard")
                        .param("category", category.getValue())
                        .param("sort", "LATEST")
                        .param("size", "10")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts").isArray())
                .andExpect(jsonPath("$.posts[0].category").value(category.getValue()))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.size").value(1));
    }

    @Test
    @DisplayName("자유게시판 글 수정 성공")
    void updatePost_Success() throws Exception {
        // given
        Long postId = 123L;
        FreeboardCreateResponse mockResponse = new FreeboardCreateResponse(postId);
        when(freeboardService.updatePost(eq(postId), any(FreeboardUpdateRequest.class), eq("testUser123")))
                .thenReturn(mockResponse);

        

        // when & then
        mockMvc.perform(multipart("/community/freeboard/{freeboardId}", postId)
                        .param("title", "수정된 제목")
                        .param("content", "수정된 내용")
                        .param("category", "LIVING_CONVENIENCE")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails))
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        })
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(postId));
    }

    @Test
    @DisplayName("자유게시판 글 삭제 성공")
    void deletePost_Success() throws Exception {
        // given
        Long postId = 123L;

        // when & then
        mockMvc.perform(delete("/community/freeboard/{freeboardId}", postId)
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andDo(print())
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("자유게시판 글 영구 삭제 성공")
    void hardDeletePost_Success() throws Exception {
        // given
        Long postId = 123L;

        // when & then
        mockMvc.perform(delete("/community/freeboard/{freeboardId}/force", postId)
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andDo(print())
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("정렬 옵션 테스트")
    void getPostsByCursor_DifferentSortOptions() throws Exception {
        // given
        FreeboardCursorResponse mockResponse = FreeboardCursorResponse.builder()
                .posts(List.of())
                .hasNext(false)
                .nextCursor(null)
                .size(0)
                .build();

        when(freeboardService.getPostsByCursor(any(), any(), anyInt(), any(), eq("testUser123")))
                .thenReturn(mockResponse);

        // when & then - 좋아요순 정렬
        mockMvc.perform(get("/community/freeboard")
                        .param("sort", "LIKE")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andDo(print())
                .andExpect(status().isOk());

        // 댓글순 정렬
        mockMvc.perform(get("/community/freeboard")
                        .param("sort", "COMMENT")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andDo(print())
                .andExpect(status().isOk());

        // 조회순 정렬
        mockMvc.perform(get("/community/freeboard")
                        .param("sort", "VIEW")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("페이지 크기 제한 테스트")
    void getPostsByCursor_PageSizeLimit() throws Exception {
        // given
        FreeboardCursorResponse mockResponse = FreeboardCursorResponse.builder()
                .posts(List.of())
                .hasNext(false)
                .nextCursor(null)
                .size(0)
                .build();

        when(freeboardService.getPostsByCursor(any(), any(), anyInt(), any(), eq("testUser123")))
                .thenReturn(mockResponse);

        // when & then - 최대 크기 초과 요청
        mockMvc.perform(get("/community/freeboard")
                        .param("size", "100") // 최대 50개 제한
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andDo(print())
                .andExpect(status().isOk());

        // 음수 크기 요청
        mockMvc.perform(get("/community/freeboard")
                        .param("size", "-5")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("인증되지 않은 사용자 요청 실패")
    void unauthenticatedRequest_ShouldFail() throws Exception {
        // given
        FreeboardCursorResponse mockResponse = FreeboardCursorResponse.builder()
                .posts(List.of())
                .hasNext(false)
                .nextCursor(null)
                .build();

        when(freeboardService.getPostsByCursor(any(), any(), anyInt(), any(), isNull()))
                .thenReturn(mockResponse);

        // when & then
        // 리스트 조회는 익명 사용자도 가능
        mockMvc.perform(get("/community/freeboard"))
                .andDo(print())
                .andExpect(status().isOk());

        // 게시글 작성은 인증 필요 (Spring Security에서 401 Unauthorized 반환)
        mockMvc.perform(post("/community/freeboard")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }
}
