package com.example.soso.community.freeboard.controller;

import com.example.soso.community.freeboard.comment.domain.dto.*;
import com.example.soso.community.freeboard.comment.service.FreeboardCommentService;
import com.example.soso.security.domain.CustomUserDetails;
import com.example.soso.users.domain.entity.Users;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
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
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.session.store-type=none"
})
@Transactional
@DisplayName("자유게시판 댓글 컨트롤러 테스트")
class FreeboardCommentControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private FreeboardCommentService commentService;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private CustomUserDetails testUserDetails;
    private final Long TEST_POST_ID = 123L;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        // 테스트용 사용자 설정
        Users testUser = Users.builder()
                .nickname("댓글러")
                .email("commenter@example.com")
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
    @DisplayName("댓글 작성 성공")
    void createComment_Success() throws Exception {
        // given
        FreeboardCommentCreateRequest request = new FreeboardCommentCreateRequest();
        request.setContent("좋은 정보 감사합니다!");

        FreeboardCommentCreateResponse mockResponse = new FreeboardCommentCreateResponse(456L);
        when(commentService.createComment(eq(TEST_POST_ID), any(FreeboardCommentCreateRequest.class), eq("testUser123")))
                .thenReturn(mockResponse);

        // when & then
        mockMvc.perform(post("/community/freeboard/{freeboardId}/comments", TEST_POST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.commentId").value(456));
    }

    @Test
    @DisplayName("대댓글 작성 성공")
    void createReply_Success() throws Exception {
        // given
        FreeboardCommentCreateRequest request = new FreeboardCommentCreateRequest();
        request.setContent("동의합니다!");
        request.setParentCommentId(789L);

        FreeboardCommentCreateResponse mockResponse = new FreeboardCommentCreateResponse(457L);
        when(commentService.createComment(eq(TEST_POST_ID), any(FreeboardCommentCreateRequest.class), eq("testUser123")))
                .thenReturn(mockResponse);

        // when & then
        mockMvc.perform(post("/community/freeboard/{freeboardId}/comments", TEST_POST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.commentId").value(457));
    }

    @Test
    @DisplayName("댓글 작성 - 빈 내용으로 실패")
    void createComment_EmptyContent_ShouldFail() throws Exception {
        // given
        FreeboardCommentCreateRequest request = new FreeboardCommentCreateRequest();
        request.setContent(""); // 빈 내용

        // when & then
        mockMvc.perform(post("/community/freeboard/{freeboardId}/comments", TEST_POST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("댓글 작성 - 너무 긴 내용으로 실패")
    void createComment_TooLongContent_ShouldFail() throws Exception {
        // given
        FreeboardCommentCreateRequest request = new FreeboardCommentCreateRequest();
        request.setContent("a".repeat(1001)); // 1000자 초과

        // when & then
        mockMvc.perform(post("/community/freeboard/{freeboardId}/comments", TEST_POST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("댓글 목록 조회 성공 - 최신순")
    void getComments_Latest_Success() throws Exception {
        // given
        FreeboardCommentCursorResponse mockResponse = FreeboardCommentCursorResponse.builder()
                .comments(List.of(
                        FreeboardCommentCursorResponse.FreeboardCommentSummary.builder()
                                .commentId(456L)
                                .postId(TEST_POST_ID)
                                .parentCommentId(null)
                                .author(FreeboardCommentCursorResponse.CommentAuthorInfo.builder()
                                        .userId("author123")
                                        .nickname("댓글작성자")
                                        .profileImageUrl("https://example.com/author.jpg")
                                        .build())
                                .content("좋은 글이네요!")
                                .replyCount(2)
                                .depth(0)
                                .deleted(false)
                                .isAuthor(false)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build()
                ))
                .hasNext(false)
                .nextCursor(null)
                .size(1)
                .build();

        when(commentService.getCommentsByCursor(eq(TEST_POST_ID), eq(FreeboardCommentSortType.LATEST), anyInt(), any(), eq("testUser123")))
                .thenReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/community/freeboard/{freeboardId}/comments", TEST_POST_ID)
                        .param("sort", "LATEST")
                        .param("size", "20")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments").isArray())
                .andExpect(jsonPath("$.comments[0].commentId").value(456))
                .andExpect(jsonPath("$.comments[0].content").value("좋은 글이네요!"))
                .andExpect(jsonPath("$.comments[0].depth").value(0))
                .andExpect(jsonPath("$.comments[0].replyCount").value(2))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    @DisplayName("댓글 목록 조회 성공 - 오래된순")
    void getComments_Oldest_Success() throws Exception {
        // given
        FreeboardCommentCursorResponse mockResponse = FreeboardCommentCursorResponse.builder()
                .comments(List.of())
                .hasNext(false)
                .nextCursor(null)
                .size(0)
                .build();

        when(commentService.getCommentsByCursor(eq(TEST_POST_ID), eq(FreeboardCommentSortType.OLDEST), anyInt(), any(), eq("testUser123")))
                .thenReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/community/freeboard/{freeboardId}/comments", TEST_POST_ID)
                        .param("sort", "OLDEST")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments").isArray());
    }

    @Test
    @DisplayName("댓글 수정 성공")
    void updateComment_Success() throws Exception {
        // given
        Long commentId = 456L;
        FreeboardCommentUpdateRequest request = new FreeboardCommentUpdateRequest();
        request.setContent("수정된 댓글 내용입니다.");

        FreeboardCommentCreateResponse mockResponse = new FreeboardCommentCreateResponse(commentId);
        when(commentService.updateComment(eq(TEST_POST_ID), eq(commentId), any(FreeboardCommentUpdateRequest.class), eq("testUser123")))
                .thenReturn(mockResponse);

        // when & then
        mockMvc.perform(patch("/community/freeboard/{freeboardId}/comments/{commentId}", TEST_POST_ID, commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentId").value(commentId));
    }

    @Test
    @DisplayName("댓글 수정 - 빈 내용으로 실패")
    void updateComment_EmptyContent_ShouldFail() throws Exception {
        // given
        Long commentId = 456L;
        FreeboardCommentUpdateRequest request = new FreeboardCommentUpdateRequest();
        request.setContent(""); // 빈 내용

        // when & then
        mockMvc.perform(patch("/community/freeboard/{freeboardId}/comments/{commentId}", TEST_POST_ID, commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("댓글 삭제 성공")
    void deleteComment_Success() throws Exception {
        // given
        Long commentId = 456L;

        // when & then
        mockMvc.perform(delete("/community/freeboard/{freeboardId}/comments/{commentId}", TEST_POST_ID, commentId)
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andDo(print())
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("댓글 영구 삭제 성공")
    void hardDeleteComment_Success() throws Exception {
        // given
        Long commentId = 456L;

        // when & then
        mockMvc.perform(delete("/community/freeboard/{freeboardId}/comments/{commentId}/force", TEST_POST_ID, commentId)
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andDo(print())
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("커서 기반 페이지네이션 테스트")
    void getComments_WithCursor() throws Exception {
        // given
        String cursor = "eyJpZCI6NDU2LCJzb3J0VmFsdWUiOiIyMDI0LTEyLTI1VDEwOjAwOjAwIn0=";
        FreeboardCommentCursorResponse mockResponse = FreeboardCommentCursorResponse.builder()
                .comments(List.of())
                .hasNext(true)
                .nextCursor("nextCursor123")
                .size(0)
                .build();

        when(commentService.getCommentsByCursor(eq(TEST_POST_ID), any(), anyInt(), eq(cursor), eq("testUser123")))
                .thenReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/community/freeboard/{freeboardId}/comments", TEST_POST_ID)
                        .param("cursor", cursor)
                        .param("size", "10")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.nextCursor").value("nextCursor123"));
    }

    @Test
    @DisplayName("페이지 크기 제한 테스트")
    void getComments_PageSizeLimit() throws Exception {
        // given
        FreeboardCommentCursorResponse mockResponse = FreeboardCommentCursorResponse.builder()
                .comments(List.of())
                .hasNext(false)
                .nextCursor(null)
                .size(0)
                .build();

        when(commentService.getCommentsByCursor(eq(TEST_POST_ID), any(), anyInt(), any(), eq("testUser123")))
                .thenReturn(mockResponse);

        // when & then - 최대 크기 초과 요청
        mockMvc.perform(get("/community/freeboard/{freeboardId}/comments", TEST_POST_ID)
                        .param("size", "100") // 최대 50개 제한
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andDo(print())
                .andExpect(status().isOk());

        // 음수 크기 요청
        mockMvc.perform(get("/community/freeboard/{freeboardId}/comments", TEST_POST_ID)
                        .param("size", "-5")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("비인증 사용자 GET 요청 성공")
    void unauthenticatedUser_GetRequest_Success() throws Exception {
        // given - Mock 서비스 설정
        FreeboardCommentCursorResponse mockResponse = FreeboardCommentCursorResponse.builder()
                .comments(List.of())
                .hasNext(false)
                .nextCursor(null)
                .size(0)
                .build();

        when(commentService.getCommentsByCursor(eq(TEST_POST_ID), any(), anyInt(), isNull(), isNull()))
                .thenReturn(mockResponse);

        // when & then - GET 요청은 비인증 사용자도 접근 가능
        mockMvc.perform(get("/community/freeboard/{freeboardId}/comments", TEST_POST_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments").isArray())
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    @DisplayName("비인증 사용자 POST 요청 차단 (401)")
    void unauthenticatedUser_PostRequest_Unauthorized() throws Exception {
        // when & then - POST 요청은 Spring Security에서 차단
        mockMvc.perform(post("/community/freeboard/{freeboardId}/comments", TEST_POST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"비인증 댓글 작성 시도\"}"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("비인증 사용자 PATCH 요청 차단 (401)")
    void unauthenticatedUser_PatchRequest_Unauthorized() throws Exception {
        // when & then - PATCH 요청은 Spring Security에서 차단
        mockMvc.perform(patch("/community/freeboard/{freeboardId}/comments/456", TEST_POST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"비인증 댓글 수정 시도\"}"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("비인증 사용자 DELETE 요청 차단 (401)")
    void unauthenticatedUser_DeleteRequest_Unauthorized() throws Exception {
        // when & then - DELETE 요청은 Spring Security에서 차단
        mockMvc.perform(delete("/community/freeboard/{freeboardId}/comments/456", TEST_POST_ID))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("계층 구조 댓글 조회 테스트")
    void getComments_HierarchicalStructure() throws Exception {
        // given
        FreeboardCommentCursorResponse mockResponse = FreeboardCommentCursorResponse.builder()
                .comments(List.of(
                        // 부모 댓글
                        FreeboardCommentCursorResponse.FreeboardCommentSummary.builder()
                                .commentId(456L)
                                .postId(TEST_POST_ID)
                                .parentCommentId(null)
                                .author(FreeboardCommentCursorResponse.CommentAuthorInfo.builder()
                                        .userId("parent123")
                                        .nickname("부모댓글작성자")
                                        .build())
                                .content("원댓글입니다.")
                                .replyCount(1)
                                .depth(0)
                                .deleted(false)
                                .isAuthor(false)
                                .createdAt(LocalDateTime.now().minusHours(1))
                                .build(),
                        // 대댓글
                        FreeboardCommentCursorResponse.FreeboardCommentSummary.builder()
                                .commentId(457L)
                                .postId(TEST_POST_ID)
                                .parentCommentId(456L)
                                .author(FreeboardCommentCursorResponse.CommentAuthorInfo.builder()
                                        .userId("child123")
                                        .nickname("대댓글작성자")
                                        .build())
                                .content("대댓글입니다.")
                                .replyCount(0)
                                .depth(1)
                                .deleted(false)
                                .isAuthor(true)
                                .createdAt(LocalDateTime.now())
                                .build()
                ))
                .hasNext(false)
                .nextCursor(null)
                .size(2)
                .build();

        when(commentService.getCommentsByCursor(eq(TEST_POST_ID), any(), anyInt(), any(), eq("testUser123")))
                .thenReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/community/freeboard/{freeboardId}/comments", TEST_POST_ID)
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments").isArray())
                .andExpect(jsonPath("$.comments[0].depth").value(0))
                .andExpect(jsonPath("$.comments[0].parentCommentId").doesNotExist())
                .andExpect(jsonPath("$.comments[0].replyCount").value(1))
                .andExpect(jsonPath("$.comments[1].depth").value(1))
                .andExpect(jsonPath("$.comments[1].parentCommentId").value(456))
                .andExpect(jsonPath("$.comments[1].replyCount").value(0))
                .andExpect(jsonPath("$.comments[1].content").value("대댓글입니다."));
    }
}