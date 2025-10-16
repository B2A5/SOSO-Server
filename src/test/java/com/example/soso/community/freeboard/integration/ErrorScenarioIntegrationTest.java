package com.example.soso.community.freeboard.integration;

import com.example.soso.community.freeboard.post.domain.dto.*;
import com.example.soso.community.freeboard.util.TestUserHelper;
import com.example.soso.community.freeboard.util.TestUserHelper.TestUser;
import com.example.soso.community.freeboard.comment.domain.dto.*;
import com.example.soso.config.TestS3Config;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@Import(TestS3Config.class)
@DisplayName("🚨 에러 시나리오 및 예외 상황 통합 테스트")
class ErrorScenarioIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private TestUserHelper testUserHelper;

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
    @DisplayName("🚫 인증 관련 에러 시나리오: 토큰 없음, 잘못된 토큰, 만료된 토큰")
    void authenticationErrorScenarios() throws Exception {
        System.out.println("\n🎬 === 인증 관련 에러 시나리오 ===");

        // ==================== 테스트 1: 토큰 없이 게시글 작성 시도 ====================
        System.out.println("\n[테스트 1] 토큰 없이 게시글 작성 시도...");

        MockMultipartFile imageFile = new MockMultipartFile(
                "images", "test.jpg", "image/jpeg", "test content".getBytes()
        );

        mockMvc.perform(multipart("/community/freeboard")
                        .file(imageFile)
                        .param("title", "무인증 게시글")
                        .param("content", "인증 없이 작성하는 게시글")
                        .param("category", "others")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.message").exists());

        // ==================== 테스트 2: 잘못된 토큰으로 요청 ====================
        System.out.println("\n[테스트 2] 잘못된 토큰으로 게시글 작성 시도...");

        mockMvc.perform(multipart("/community/freeboard")
                        .file(imageFile)
                        .param("title", "잘못된 토큰 게시글")
                        .param("content", "잘못된 토큰으로 작성하는 게시글")
                        .param("category", "others")
                        .header("Authorization", "Bearer invalid-token-here")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.message").exists());

        // ==================== 테스트 3: 정상 사용자는 인증 필요 없는 조회는 가능 ====================
        System.out.println("\n[테스트 3] 미인증 상태에서 게시글 목록 조회...");

        mockMvc.perform(get("/community/freeboard")
                        .param("category", "others")
                        .param("sort", "LATEST")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts").isArray());

        System.out.println("✅ 미인증 상태 게시글 목록 조회 성공!");

        System.out.println("\n🎉 === 인증 관련 에러 시나리오 완료 ===");
    }

    @Test
    @DisplayName("📝 데이터 검증 에러 시나리오: 필수 필드 누락, 길이 제한 초과, 잘못된 형식")
    void dataValidationErrorScenarios() throws Exception {
        System.out.println("\n🎬 === 데이터 검증 에러 시나리오 ===");

        TestUser validUser = testUserHelper.createFounderUser();

        // ==================== 테스트 1: 제목 없이 게시글 작성 ====================
        System.out.println("\n[테스트 1] 제목 없이 게시글 작성 시도...");

        MockMultipartFile imageFile = new MockMultipartFile(
                "images", "test.jpg", "image/jpeg", "test content".getBytes()
        );

        mockMvc.perform(multipart("/community/freeboard")
                        .file(imageFile)
                        .param("title", "") // 빈 제목
                        .param("content", "내용은 있지만 제목이 없는 게시글")
                        .param("category", "others")
                        .header("Authorization", validUser.getAuthHeader())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").exists());


        // ==================== 테스트 2: 내용 없이 게시글 작성 ====================
        System.out.println("\n[테스트 2] 내용 없이 게시글 작성 시도...");

        mockMvc.perform(multipart("/community/freeboard")
                        .file(imageFile)
                        .param("title", "제목은 있는 게시글")
                        .param("content", "") // 빈 내용
                        .param("category", "others")
                        .header("Authorization", validUser.getAuthHeader())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").exists());

        // ==================== 테스트 3: 잘못된 카테고리로 게시글 작성 ====================
        System.out.println("\n[테스트 3] 잘못된 카테고리로 게시글 작성 시도...");

        mockMvc.perform(multipart("/community/freeboard")
                        .file(imageFile)
                        .param("title", "정상 제목")
                        .param("content", "정상 내용")
                        .param("category", "invalid-category") // 존재하지 않는 카테고리
                        .header("Authorization", validUser.getAuthHeader())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isBadRequest());

        // ==================== 테스트 4: 너무 긴 제목으로 게시글 작성 ====================
        System.out.println("\n[테스트 4] 너무 긴 제목으로 게시글 작성 시도...");

        String longTitle = "글".repeat(101); // 100자 초과

        mockMvc.perform(multipart("/community/freeboard")
                        .file(imageFile)
                        .param("title", longTitle)
                        .param("content", "정상 내용")
                        .param("category", "others")
                        .header("Authorization", validUser.getAuthHeader())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").exists());

        System.out.println("\n🎉 === 데이터 검증 에러 시나리오 완료 ===");
    }

    @Test
    @DisplayName("🔐 권한 관련 에러 시나리오: 다른 사용자 게시글/댓글 수정/삭제 시도")
    void authorizationErrorScenarios() throws Exception {
        System.out.println("\n🎬 === 권한 관련 에러 시나리오 ===");

        // ==================== 준비: 게시글과 댓글 작성 ====================
        TestUser postAuthor = testUserHelper.createFounderUser();
        TestUser otherUser = testUserHelper.createInhabitantUser();

        System.out.println("게시글 작성자: " + postAuthor.getNickname());
        System.out.println("다른 사용자: " + otherUser.getNickname());

        // 게시글 작성
        MockMultipartFile imageFile = new MockMultipartFile(
                "images", "test.jpg", "image/jpeg", "test content".getBytes()
        );

        MvcResult postResult = mockMvc.perform(multipart("/community/freeboard")
                        .file(imageFile)
                        .param("title", "권한 테스트용 게시글")
                        .param("content", "이 게시글은 권한 테스트용입니다.")
                        .param("category", "others")
                        .header("Authorization", postAuthor.getAuthHeader())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        FreeboardCreateResponse createResponse = objectMapper.readValue(
                postResult.getResponse().getContentAsString(), FreeboardCreateResponse.class);
        Long postId = createResponse.getPostId();

        // 댓글 작성
        MvcResult commentResult = mockMvc.perform(post("/community/freeboard/{freeboardId}/comments", postId)
                        .header("Authorization", postAuthor.getAuthHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"권한 테스트용 댓글입니다.\"}"))
                .andDo(print())
                .andExpect(status().isCreated())
                .andReturn();

        FreeboardCommentCreateResponse commentResponse = objectMapper.readValue(
                commentResult.getResponse().getContentAsString(), FreeboardCommentCreateResponse.class);
        Long commentId = commentResponse.getCommentId();

        System.out.println("✅ 테스트 데이터 준비 완료: 게시글 ID=" + postId + ", 댓글 ID=" + commentId);

        // ==================== 테스트 1: 다른 사용자가 게시글 수정 시도 ====================
        System.out.println("\n[테스트 1] 다른 사용자의 게시글 수정 시도...");

        mockMvc.perform(multipart("/community/freeboard/{freeboardId}", postId)
                        .param("title", "해킹 시도 제목")
                        .param("content", "다른 사용자가 수정하려는 내용")
                        .param("category", "others")
                        .header("Authorization", otherUser.getAuthHeader())
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        })
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("POST_ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").exists());

        System.out.println("✅ 게시글 수정 권한 차단 성공!");

        // ==================== 테스트 2: 다른 사용자가 게시글 삭제 시도 ====================
        System.out.println("\n[테스트 2] 다른 사용자의 게시글 삭제 시도...");

        mockMvc.perform(delete("/community/freeboard/{freeboardId}", postId)
                        .header("Authorization", otherUser.getAuthHeader()))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("POST_ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").exists());

        System.out.println("✅ 게시글 삭제 권한 차단 성공!");

        // ==================== 테스트 3: 다른 사용자가 댓글 수정 시도 ====================
        System.out.println("\n[테스트 3] 다른 사용자의 댓글 수정 시도...");

        mockMvc.perform(patch("/community/freeboard/{freeboardId}/comments/{commentId}", postId, commentId)
                        .header("Authorization", otherUser.getAuthHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"해킹된 댓글 내용\"}"))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMMENT_ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").exists());

        System.out.println("✅ 댓글 수정 권한 차단 성공!");

        // ==================== 테스트 4: 다른 사용자가 댓글 삭제 시도 ====================
        System.out.println("\n[테스트 4] 다른 사용자의 댓글 삭제 시도...");

        mockMvc.perform(delete("/community/freeboard/{freeboardId}/comments/{commentId}", postId, commentId)
                        .header("Authorization", otherUser.getAuthHeader()))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMMENT_ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").exists());

        System.out.println("✅ 댓글 삭제 권한 차단 성공!");

        // ==================== 테스트 5: 정당한 소유자는 수정/삭제 가능 확인 ====================
        System.out.println("\n[테스트 5] 정당한 소유자의 수정/삭제는 정상 작동 확인...");

        // 본인 댓글 수정은 성공해야 함
        mockMvc.perform(patch("/community/freeboard/{freeboardId}/comments/{commentId}", postId, commentId)
                        .header("Authorization", postAuthor.getAuthHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"정당한 소유자가 수정한 댓글\"}"))
                .andDo(print())
                .andExpect(status().isOk());

        System.out.println("✅ 정당한 소유자 권한 확인 성공!");

        System.out.println("\n🎉 === 권한 관련 에러 시나리오 완료 ===");
    }

    @Test
    @DisplayName("🔍 리소스 존재하지 않음 에러 시나리오: 404 Not Found 케이스들")
    void resourceNotFoundErrorScenarios() throws Exception {
        System.out.println("\n🎬 === 리소스 존재하지 않음 에러 시나리오 ===");

        TestUser validUser = testUserHelper.createFounderUser();

        // ==================== 테스트 1: 존재하지 않는 게시글 조회 ====================
        System.out.println("\n[테스트 1] 존재하지 않는 게시글 조회 시도...");

        mockMvc.perform(get("/community/freeboard/{freeboardId}", 99999L)
                        .header("Authorization", validUser.getAuthHeader()))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"))
                .andExpect(jsonPath("$.message").exists());

        System.out.println("✅ 존재하지 않는 게시글 조회 에러 처리 성공!");

        // ==================== 테스트 2: 존재하지 않는 게시글 수정 시도 ====================
        System.out.println("\n[테스트 2] 존재하지 않는 게시글 수정 시도...");

        mockMvc.perform(multipart("/community/freeboard/{freeboardId}", 99999L)
                        .param("title", "존재하지 않는 게시글 수정")
                        .param("content", "수정 내용")
                        .param("category", "others")
                        .header("Authorization", validUser.getAuthHeader())
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        })
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"))
                .andExpect(jsonPath("$.message").exists());

        System.out.println("✅ 존재하지 않는 게시글 수정 에러 처리 성공!");

        // ==================== 테스트 3: 존재하지 않는 게시글에 댓글 작성 시도 ====================
        System.out.println("\n[테스트 3] 존재하지 않는 게시글에 댓글 작성 시도...");

        mockMvc.perform(post("/community/freeboard/{freeboardId}/comments", 99999L)
                        .header("Authorization", validUser.getAuthHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"존재하지 않는 게시글에 댓글\"}"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"))
                .andExpect(jsonPath("$.message").exists());

        System.out.println("✅ 존재하지 않는 게시글 댓글 작성 에러 처리 성공!");

        // ==================== 테스트 4: 존재하지 않는 댓글 수정 시도 ====================
        System.out.println("\n[테스트 4] 존재하지 않는 댓글 수정 시도...");

        // 먼저 실제 게시글을 생성
        MockMultipartFile imageFile = new MockMultipartFile(
                "images", "test.jpg", "image/jpeg", "test content".getBytes()
        );

        MvcResult postResult = mockMvc.perform(multipart("/community/freeboard")
                        .file(imageFile)
                        .param("title", "댓글 테스트용 게시글")
                        .param("content", "댓글 테스트를 위한 게시글")
                        .param("category", "others")
                        .header("Authorization", validUser.getAuthHeader())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        FreeboardCreateResponse createResponse = objectMapper.readValue(
                postResult.getResponse().getContentAsString(), FreeboardCreateResponse.class);
        Long validPostId = createResponse.getPostId();

        // 존재하지 않는 댓글 수정 시도
        mockMvc.perform(patch("/community/freeboard/{freeboardId}/comments/{commentId}", validPostId, 99999L)
                        .header("Authorization", validUser.getAuthHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"존재하지 않는 댓글 수정\"}"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").exists());

        System.out.println("✅ 존재하지 않는 댓글 수정 에러 처리 성공!");

        System.out.println("\n🎉 === 리소스 존재하지 않음 에러 시나리오 완료 ===");
    }

    @Test
    @DisplayName("⚠️ 비즈니스 로직 에러 시나리오: 중복 좋아요, 삭제된 리소스 접근 등")
    void businessLogicErrorScenarios() throws Exception {
        System.out.println("\n🎬 === 비즈니스 로직 에러 시나리오 ===");

        TestUser user1 = testUserHelper.createFounderUser();
        TestUser user2 = testUserHelper.createInhabitantUser();

        // 게시글 생성
        MockMultipartFile imageFile = new MockMultipartFile(
                "images", "test.jpg", "image/jpeg", "test content".getBytes()
        );

        MvcResult postResult = mockMvc.perform(multipart("/community/freeboard")
                        .file(imageFile)
                        .param("title", "비즈니스 로직 테스트 게시글")
                        .param("content", "비즈니스 로직 테스트를 위한 게시글")
                        .param("category", "others")
                        .header("Authorization", user1.getAuthHeader())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        FreeboardCreateResponse createResponse = objectMapper.readValue(
                postResult.getResponse().getContentAsString(), FreeboardCreateResponse.class);
        Long postId = createResponse.getPostId();

        // ==================== 테스트 1: 중복 좋아요 시도 ====================
        System.out.println("\n[테스트 1] 중복 좋아요 시도...");

        // 첫 번째 좋아요 (성공해야 함)
        mockMvc.perform(post("/community/freeboard/{freeboardId}/like", postId)
                        .header("Authorization", user2.getAuthHeader()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("true")); // 좋아요 추가됨

        // 두 번째 좋아요 토글 시도 (좋아요 취소, 200 OK 반환)
        mockMvc.perform(post("/community/freeboard/{freeboardId}/like", postId)
                        .header("Authorization", user2.getAuthHeader()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("false")); // 좋아요 취소됨

        System.out.println("✅ 좋아요 토글 동작 확인 성공!");

        // ==================== 테스트 2: 토글 방식 좋아요 취소 확인 ====================
        System.out.println("\n[테스트 2] user1이 좋아요 추가 후 토글로 취소 테스트...");

        // user1이 좋아요 추가
        mockMvc.perform(post("/community/freeboard/{freeboardId}/like", postId)
                        .header("Authorization", user1.getAuthHeader()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("true")); // 좋아요 추가됨

        // user1이 다시 토글하여 좋아요 취소
        mockMvc.perform(post("/community/freeboard/{freeboardId}/like", postId)
                        .header("Authorization", user1.getAuthHeader()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("false")); // 좋아요 취소됨

        System.out.println("✅ 토글 방식 좋아요 추가/취소 동작 확인 성공!");

        // ==================== 테스트 3: 삭제된 게시글 접근 시도 ====================
        System.out.println("\n[테스트 3] 삭제된 게시글 접근 시도...");

        // 게시글 삭제 (소프트 삭제)
        mockMvc.perform(delete("/community/freeboard/{freeboardId}", postId)
                        .header("Authorization", user1.getAuthHeader()))
                .andDo(print())
                .andExpect(status().isNoContent());

        // 삭제된 게시글 조회 시도
        mockMvc.perform(get("/community/freeboard/{freeboardId}", postId)
                        .header("Authorization", user2.getAuthHeader()))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"))
                .andExpect(jsonPath("$.message").exists());

        System.out.println("✅ 삭제된 게시글 접근 에러 처리 성공!");

        // ==================== 테스트 4: 삭제된 게시글에 댓글 작성 시도 ====================
        System.out.println("\n[테스트 4] 삭제된 게시글에 댓글 작성 시도...");

        mockMvc.perform(post("/community/freeboard/{freeboardId}/comments", postId)
                        .header("Authorization", user2.getAuthHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"삭제된 게시글에 댓글 시도\"}"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"))
                .andExpect(jsonPath("$.message").exists());

        System.out.println("✅ 삭제된 게시글 댓글 작성 에러 처리 성공!");

        System.out.println("\n🎉 === 비즈니스 로직 에러 시나리오 완료 ===");
    }

    @Test
    @DisplayName("📄 페이징 및 파라미터 에러 시나리오: 잘못된 파라미터, 범위 초과 등")
    void paginationAndParameterErrorScenarios() throws Exception {
        System.out.println("\n🎬 === 페이징 및 파라미터 에러 시나리오 ===");

        TestUser validUser = testUserHelper.createFounderUser();

        // ==================== 테스트 1: 잘못된 정렬 옵션 ====================
        System.out.println("\n[테스트 1] 잘못된 정렬 옵션으로 게시글 목록 조회...");

        mockMvc.perform(get("/community/freeboard")
                        .param("sort", "INVALID_SORT") // 존재하지 않는 정렬 옵션
                        .param("size", "10")
                        .header("Authorization", validUser.getAuthHeader()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ENUM_VALUE"))
                .andExpect(jsonPath("$.message").exists());

        System.out.println("✅ 잘못된 정렬 옵션 에러 처리 성공!");


        // ==================== 테스트 4: 잘못된 커서 값 ====================
        System.out.println("\n[테스트 4] 잘못된 커서 값으로 조회...");

        mockMvc.perform(get("/community/freeboard")
                        .param("sort", "LATEST")
                        .param("size", "10")
                        .param("cursor", "invalid-cursor-format") // 잘못된 커서
                        .header("Authorization", validUser.getAuthHeader()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CURSOR"))
                .andExpect(jsonPath("$.message").exists());

        System.out.println("✅ 잘못된 커서 값 에러 처리 성공!");

        System.out.println("\n🎉 === 페이징 및 파라미터 에러 시나리오 완료 ===");
    }
}