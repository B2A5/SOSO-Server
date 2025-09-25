package com.example.soso.community.freeboard.integration;

import com.example.soso.community.freeboard.post.domain.dto.*;
import com.example.soso.community.freeboard.comment.domain.dto.*;
import com.example.soso.community.common.post.domain.entity.Category;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

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
@DisplayName("자유게시판 통합 테스트")
class FreeboardIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithMockUser(username = "testUser", roles = "USER")
    @DisplayName("자유게시판 전체 플로우 테스트: 게시글 작성 → 조회 → 댓글 작성 → 수정 → 삭제")
    void completeWorkflow_Success() throws Exception {
        // 1. 게시글 작성
        MockMultipartFile titlePart = new MockMultipartFile("title", "", "text/plain", "통합테스트 게시글".getBytes());
        MockMultipartFile contentPart = new MockMultipartFile("content", "", "text/plain", "통합테스트를 위한 게시글입니다.".getBytes());
        MockMultipartFile categoryPart = new MockMultipartFile("category", "", "text/plain", "daily-hobby".getBytes());
        MockMultipartFile imagePart = new MockMultipartFile("images", "test.jpg", "image/jpeg", "test image content".getBytes());

        MvcResult createResult = mockMvc.perform(multipart("/community/freeboard")
                        .file(titlePart)
                        .file(contentPart)
                        .file(categoryPart)
                        .file(imagePart)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").exists())
                .andReturn();

        // 게시글 ID 추출
        String createResponseContent = createResult.getResponse().getContentAsString();
        FreeboardCreateResponse createResponse = objectMapper.readValue(createResponseContent, FreeboardCreateResponse.class);
        Long postId = createResponse.getPostId();

        // 2. 게시글 상세 조회
        mockMvc.perform(get("/community/freeboard/{freeboardId}", postId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(postId))
                .andExpect(jsonPath("$.title").value("통합테스트 게시글"))
                .andExpect(jsonPath("$.category").value("daily-hobby"))
                .andExpect(jsonPath("$.author.userId").value("testUser"))
                .andExpect(jsonPath("$.isAuthor").value(true));

        // 3. 게시글 목록 조회 (전체)
        mockMvc.perform(get("/community/freeboard")
                        .param("sort", "LATEST")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts").isArray())
                .andExpect(jsonPath("$.posts[0].postId").value(postId));

        // 4. 카테고리별 조회
        mockMvc.perform(get("/community/freeboard")
                        .param("category", "daily-hobby")
                        .param("sort", "LATEST"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts").isArray())
                .andExpect(jsonPath("$.posts[0].category").value("daily-hobby"));

        // 5. 댓글 작성
        FreeboardCommentCreateRequest commentRequest = new FreeboardCommentCreateRequest();
        commentRequest.setContent("좋은 글이네요!");

        MvcResult commentResult = mockMvc.perform(post("/community/freeboard/{freeboardId}/comments", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.commentId").exists())
                .andReturn();

        // 댓글 ID 추출
        String commentResponseContent = commentResult.getResponse().getContentAsString();
        FreeboardCommentCreateResponse commentResponse = objectMapper.readValue(commentResponseContent, FreeboardCommentCreateResponse.class);
        Long commentId = commentResponse.getCommentId();

        // 6. 댓글 목록 조회
        mockMvc.perform(get("/community/freeboard/{freeboardId}/comments", postId)
                        .param("sort", "LATEST"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments").isArray())
                .andExpect(jsonPath("$.comments[0].commentId").value(commentId))
                .andExpect(jsonPath("$.comments[0].content").value("좋은 글이네요!"));

        // 7. 대댓글 작성
        FreeboardCommentCreateRequest replyRequest = new FreeboardCommentCreateRequest();
        replyRequest.setContent("동감합니다!");
        replyRequest.setParentCommentId(commentId);

        mockMvc.perform(post("/community/freeboard/{freeboardId}/comments", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(replyRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.commentId").exists());

        // 8. 댓글 수정
        FreeboardCommentUpdateRequest updateCommentRequest = new FreeboardCommentUpdateRequest();
        updateCommentRequest.setContent("수정된 댓글입니다!");

        mockMvc.perform(patch("/community/freeboard/{freeboardId}/comments/{commentId}", postId, commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateCommentRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentId").value(commentId));

        // 9. 게시글 수정
        MockMultipartFile updatedTitlePart = new MockMultipartFile("title", "", "text/plain", "수정된 제목".getBytes());
        MockMultipartFile updatedContentPart = new MockMultipartFile("content", "", "text/plain", "수정된 내용입니다.".getBytes());
        MockMultipartFile updatedCategoryPart = new MockMultipartFile("category", "", "text/plain", "restaurant".getBytes());

        mockMvc.perform(multipart("/community/freeboard/{freeboardId}", postId)
                        .file(updatedTitlePart)
                        .file(updatedContentPart)
                        .file(updatedCategoryPart)
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        })
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(postId));

        // 10. 수정된 게시글 확인
        mockMvc.perform(get("/community/freeboard/{freeboardId}", postId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("수정된 제목"))
                .andExpect(jsonPath("$.content").value("수정된 내용입니다."))
                .andExpect(jsonPath("$.category").value("restaurant"));

        // 11. 댓글 삭제 (소프트 삭제)
        mockMvc.perform(delete("/community/freeboard/{freeboardId}/comments/{commentId}", postId, commentId))
                .andDo(print())
                .andExpect(status().isNoContent());

        // 12. 게시글 삭제 (소프트 삭제)
        mockMvc.perform(delete("/community/freeboard/{freeboardId}", postId))
                .andDo(print())
                .andExpect(status().isNoContent());

        // 13. 삭제된 게시글 조회 시도 (404 에러 예상)
        mockMvc.perform(get("/community/freeboard/{freeboardId}", postId))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testUser", roles = "USER")
    @DisplayName("카테고리별 게시글 작성 및 필터링 테스트")
    void categoryFilteringWorkflow_Success() throws Exception {
        // 다양한 카테고리의 게시글 작성
        Category[] categories = {Category.RESTAURANT, Category.DAILY_HOBBY, Category.STARTUP};
        Long[] postIds = new Long[categories.length];

        for (int i = 0; i < categories.length; i++) {
            Category category = categories[i];

            MockMultipartFile titlePart = new MockMultipartFile("title", "", "text/plain",
                    (category.getLabel() + " 테스트 제목").getBytes());
            MockMultipartFile contentPart = new MockMultipartFile("content", "", "text/plain",
                    (category.getLabel() + " 관련 내용입니다.").getBytes());
            MockMultipartFile categoryPart = new MockMultipartFile("category", "", "text/plain",
                    category.getValue().getBytes());

            MvcResult result = mockMvc.perform(multipart("/community/freeboard")
                            .file(titlePart)
                            .file(contentPart)
                            .file(categoryPart)
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.postId").exists())
                    .andReturn();

            String responseContent = result.getResponse().getContentAsString();
            FreeboardCreateResponse response = objectMapper.readValue(responseContent, FreeboardCreateResponse.class);
            postIds[i] = response.getPostId();
        }

        // 각 카테고리별로 필터링 테스트
        for (int i = 0; i < categories.length; i++) {
            Category category = categories[i];

            mockMvc.perform(get("/community/freeboard")
                            .param("category", category.getValue())
                            .param("sort", "LATEST"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.posts").isArray())
                    .andExpect(jsonPath("$.posts[0].postId").value(postIds[i]))
                    .andExpect(jsonPath("$.posts[0].category").value(category.getValue()));
        }

        // 전체 카테고리 조회 (3개 모두 포함되어야 함)
        mockMvc.perform(get("/community/freeboard")
                        .param("sort", "LATEST")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts").isArray())
                .andExpect(jsonPath("$.posts.length()").value(3));
    }

    @Test
    @WithMockUser(username = "testUser", roles = "USER")
    @DisplayName("정렬 옵션별 게시글 조회 테스트")
    void sortingOptionsWorkflow_Success() throws Exception {
        // 테스트용 게시글 작성
        MockMultipartFile titlePart = new MockMultipartFile("title", "", "text/plain", "정렬 테스트".getBytes());
        MockMultipartFile contentPart = new MockMultipartFile("content", "", "text/plain", "정렬 테스트용 게시글입니다.".getBytes());
        MockMultipartFile categoryPart = new MockMultipartFile("category", "", "text/plain", "daily-hobby".getBytes());

        mockMvc.perform(multipart("/community/freeboard")
                        .file(titlePart)
                        .file(contentPart)
                        .file(categoryPart)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk());

        // 각 정렬 옵션 테스트
        for (FreeboardSortType sortType : FreeboardSortType.values()) {
            mockMvc.perform(get("/community/freeboard")
                            .param("sort", sortType.name())
                            .param("size", "10"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.posts").isArray());
        }
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    @DisplayName("권한 검증 테스트 - 다른 사용자의 게시글/댓글 수정/삭제 시도")
    void authorizationTest() throws Exception {
        // user1이 게시글 작성
        MockMultipartFile titlePart = new MockMultipartFile("title", "", "text/plain", "user1의 게시글".getBytes());
        MockMultipartFile contentPart = new MockMultipartFile("content", "", "text/plain", "user1이 작성한 내용".getBytes());
        MockMultipartFile categoryPart = new MockMultipartFile("category", "", "text/plain", "daily-hobby".getBytes());

        MvcResult createResult = mockMvc.perform(multipart("/community/freeboard")
                        .file(titlePart)
                        .file(contentPart)
                        .file(categoryPart)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = createResult.getResponse().getContentAsString();
        FreeboardCreateResponse response = objectMapper.readValue(responseContent, FreeboardCreateResponse.class);
        Long postId = response.getPostId();

        // user1이 댓글 작성
        FreeboardCommentCreateRequest commentRequest = new FreeboardCommentCreateRequest();
        commentRequest.setContent("user1의 댓글입니다.");

        MvcResult commentResult = mockMvc.perform(post("/community/freeboard/{freeboardId}/comments", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String commentResponseContent = commentResult.getResponse().getContentAsString();
        FreeboardCommentCreateResponse commentResponse = objectMapper.readValue(commentResponseContent, FreeboardCommentCreateResponse.class);
        Long commentId = commentResponse.getCommentId();

        // TODO: user2로 전환하여 user1의 게시글/댓글 수정/삭제 시도
        // (실제로는 별도 테스트 메서드나 @WithMockUser 조정 필요)

        // 현재는 조회는 성공해야 함
        mockMvc.perform(get("/community/freeboard/{freeboardId}", postId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.author.userId").value("user1"))
                .andExpect(jsonPath("$.isAuthor").value(true)); // 같은 사용자이므로 true
    }

    @Test
    @DisplayName("인증되지 않은 사용자 접근 테스트")
    void unauthenticatedAccessTest() throws Exception {
        // 인증 없이 게시글 목록 조회 시도
        mockMvc.perform(get("/community/freeboard"))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        // 인증 없이 게시글 작성 시도
        mockMvc.perform(post("/community/freeboard")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        // 인증 없이 댓글 작성 시도
        mockMvc.perform(post("/community/freeboard/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }
}