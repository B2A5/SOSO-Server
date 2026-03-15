package com.example.soso.community.freeboard.integration;

import com.example.soso.community.freeboard.comment.domain.dto.*;
import com.example.soso.community.freeboard.post.domain.dto.*;
import com.example.soso.community.freeboard.util.TestUserHelper;
import com.example.soso.config.TestRedisConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Import(TestRedisConfig.class)
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
@DisplayName("💬 댓글 시스템 통합 테스트")
class CommentSystemIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestUserHelper testUserHelper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("💬 완전한 댓글 시나리오: 댓글 → 대댓글 → 수정 → 삭제 → 좋아요")
    void completeCommentLifecycleScenario() throws Exception {
        System.out.println("\n💬 완전한 댓글 라이프사이클 시나리오 테스트 시작...");

        // ==================== 준비 단계 ====================
        TestUserHelper.TestUser postAuthor = testUserHelper.createFounderUser();
        TestUserHelper.TestUser commenter1 = testUserHelper.createInhabitantUser();
        TestUserHelper.TestUser commenter2 = testUserHelper.createFounderUser();

        System.out.println("👥 참여자 준비:");
        System.out.println("  - 게시글 작성자: " + postAuthor.getNickname() + " (FOUNDER)");
        System.out.println("  - 댓글러1: " + commenter1.getNickname() + " (INHABITANT)");
        System.out.println("  - 댓글러2: " + commenter2.getNickname() + " (FOUNDER)");

        // 테스트용 게시글 생성
        Long postId = createTestPost(postAuthor, "🚀 스타트업 창업 Q&A 모음집", "STARTUP");
        System.out.println("📝 게시글 준비 완료: ID=" + postId);

        // ==================== STEP 1: 첫 번째 댓글 작성 ====================
        System.out.println("\n[STEP 1] 첫 번째 댓글 작성...");
        String comment1Content = "정말 유용한 정보네요! 특히 투자 유치 부분이 인상 깊었어요. " +
                "저도 창업을 준비 중인데 많은 도움이 됐습니다 👍";

        MvcResult comment1Result = mockMvc.perform(post("/community/freeboard/{freeboardId}/comments", postId)
                        .header("Authorization", commenter1.getAuthHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"" + comment1Content + "\"}"))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.commentId").exists())
                .andReturn();

        FreeboardCommentCreateResponse comment1Response = objectMapper.readValue(
            comment1Result.getResponse().getContentAsString(), FreeboardCommentCreateResponse.class);
        Long comment1Id = comment1Response.getCommentId();

        System.out.println("✅ 첫 번째 댓글 작성 완료: ID=" + comment1Id);

        // ==================== STEP 2: 두 번째 댓글 작성 ====================
        System.out.println("\n[STEP 2] 두 번째 댓글 작성...");
        String comment2Content = "저도 창업가인데, 이런 실전 경험담이 정말 필요했어요! " +
                "혹시 멘토링도 해주시나요? 😊";

        MvcResult comment2Result = mockMvc.perform(post("/community/freeboard/{freeboardId}/comments", postId)
                        .header("Authorization", commenter2.getAuthHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"" + comment2Content + "\"}"))
                .andDo(print())
                .andExpect(status().isCreated())
                .andReturn();

        FreeboardCommentCreateResponse comment2Response = objectMapper.readValue(
            comment2Result.getResponse().getContentAsString(), FreeboardCommentCreateResponse.class);
        Long comment2Id = comment2Response.getCommentId();

        System.out.println("✅ 두 번째 댓글 작성 완료: ID=" + comment2Id);

        // ==================== STEP 3: 대댓글 작성 ====================
        System.out.println("\n[STEP 3] 게시글 작성자가 대댓글 작성...");
        String replyContent = "@" + commenter2.getNickname() + " 네! 언제든지 DM 주세요. " +
                "서로 정보 공유하며 함께 성장해요! 창업 파이팅 💪";

        MvcResult replyResult = mockMvc.perform(post("/community/freeboard/{freeboardId}/comments", postId)
                        .header("Authorization", postAuthor.getAuthHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"" + replyContent + "\", \"parentCommentId\": " + comment2Id + "}"))
                .andDo(print())
                .andExpect(status().isCreated())
                .andReturn();

        FreeboardCommentCreateResponse replyResponse = objectMapper.readValue(
            replyResult.getResponse().getContentAsString(), FreeboardCommentCreateResponse.class);
        Long replyId = replyResponse.getCommentId();

        System.out.println("✅ 대댓글 작성 완료: ID=" + replyId);

        // ==================== STEP 4: 댓글 목록 조회 및 구조 확인 ====================
        System.out.println("\n[STEP 4] 댓글 목록 조회 및 계층 구조 확인...");
        MvcResult commentsListResult = mockMvc.perform(get("/community/freeboard/{freeboardId}/comments", postId)
                        .param("sort", "LATEST")
                        .param("size", "20"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments").isArray())
                .andExpect(jsonPath("$.comments").isNotEmpty())
                .andReturn();

        FreeboardCommentCursorResponse commentsList = objectMapper.readValue(
            commentsListResult.getResponse().getContentAsString(), FreeboardCommentCursorResponse.class);

        System.out.println("📊 댓글 현황:");
        System.out.println("  - 총 댓글 수: " + commentsList.getComments().size());
        commentsList.getComments().forEach(comment -> {
            System.out.println("  - " + comment.getCommentId() + ": " +
                             (comment.getParentCommentId() != null ? "(대댓글)" : "(일반댓글)") +
                             " by " + comment.getAuthor().getNickname());
        });

        // ==================== STEP 5: 댓글 수정 ====================
        System.out.println("\n[STEP 5] 첫 번째 댓글 수정...");
        String updatedComment1Content = comment1Content + "\n\n" +
                "[수정됨] 추가로 궁금한 점이 있는데, 초기 자금은 어떻게 조달하셨나요?";

        String updateRequestJson = objectMapper.writeValueAsString(Map.of("content", updatedComment1Content));

        mockMvc.perform(patch("/community/freeboard/{freeboardId}/comments/{commentId}", postId, comment1Id)
                        .header("Authorization", commenter1.getAuthHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequestJson))
                .andDo(print())
                .andExpect(status().isOk());

        System.out.println("✅ 댓글 수정 완료");

        // ==================== STEP 6: 댓글 좋아요 ====================
        System.out.println("\n[STEP 6] 댓글 좋아요 기능 테스트...");

        // 첫 번째 댓글에 좋아요
        mockMvc.perform(post("/community/freeboard/{freeboardId}/comments/{commentId}/like", postId, comment1Id)
                        .header("Authorization", commenter2.getAuthHeader()))
                .andDo(print())
                .andExpect(status().isOk());

        // 두 번째 댓글에도 좋아요
        mockMvc.perform(post("/community/freeboard/{freeboardId}/comments/{commentId}/like", postId, comment2Id)
                        .header("Authorization", commenter1.getAuthHeader()))
                .andDo(print())
                .andExpect(status().isOk());

        System.out.println("✅ 댓글 좋아요 완료");

        // ==================== STEP 7: 권한 테스트 (다른 사용자의 댓글 수정/삭제 시도) ====================
        System.out.println("\n[STEP 7] 댓글 권한 테스트...");

        // 다른 사용자가 댓글 수정 시도 - 403 Forbidden
        mockMvc.perform(patch("/community/freeboard/{freeboardId}/comments/{commentId}", postId, comment1Id)
                        .header("Authorization", commenter2.getAuthHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"다른 사용자의 수정 시도\"}"))
                .andDo(print())
                .andExpect(status().isForbidden());

        // 다른 사용자가 댓글 삭제 시도 - 403 Forbidden
        mockMvc.perform(delete("/community/freeboard/{freeboardId}/comments/{commentId}", postId, comment1Id)
                        .header("Authorization", commenter2.getAuthHeader()))
                .andDo(print())
                .andExpect(status().isForbidden());

        System.out.println("✅ 댓글 권한 제한 확인 완료");

        // ==================== STEP 8: 댓글 삭제 (소프트 삭제) ====================
        System.out.println("\n[STEP 8] 댓글 소프트 삭제 테스트...");

        // 두 번째 댓글 삭제 (대댓글이 있는 댓글)
        mockMvc.perform(delete("/community/freeboard/{freeboardId}/comments/{commentId}", postId, comment2Id)
                        .header("Authorization", commenter2.getAuthHeader()))
                .andDo(print())
                .andExpect(status().isNoContent());

        System.out.println("✅ 댓글 삭제 완료 (대댓글이 있는 부모 댓글)");

        // ==================== STEP 9: 삭제 후 댓글 목록 확인 ====================
        System.out.println("\n[STEP 9] 삭제 후 댓글 목록 상태 확인...");
        MvcResult afterDeleteResult = mockMvc.perform(get("/community/freeboard/{freeboardId}/comments", postId)
                        .param("sort", "LATEST")
                        .param("size", "20"))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        FreeboardCommentCursorResponse afterDeleteList = objectMapper.readValue(
            afterDeleteResult.getResponse().getContentAsString(), FreeboardCommentCursorResponse.class);

        System.out.println("📊 삭제 후 댓글 상태:");
        afterDeleteList.getComments().forEach(comment -> {
            System.out.println("  - " + comment.getCommentId() + ": " +
                             (comment.isDeleted() ? "[삭제됨]" : comment.getContent().substring(0, Math.min(20, comment.getContent().length()))) +
                             " by " + comment.getAuthor().getNickname());
        });

        // ==================== STEP 10: 게시글 상태에서 댓글 수 확인 ====================
        System.out.println("\n[STEP 10] 게시글의 댓글 수 업데이트 확인...");
        MvcResult postResult = mockMvc.perform(get("/community/freeboard/{freeboardId}", postId)
                        .header("Authorization", postAuthor.getAuthHeader()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentCount").exists())
                .andReturn();

        FreeboardDetailResponse postDetail = objectMapper.readValue(
            postResult.getResponse().getContentAsString(), FreeboardDetailResponse.class);

        System.out.println("📊 게시글 최종 상태:");
        System.out.println("  - 댓글 수: " + postDetail.getCommentCount());

        // ==================== 검증 ====================
        assertThat(comment1Id).isNotNull();
        assertThat(comment2Id).isNotNull();
        assertThat(replyId).isNotNull();
        assertThat(commentsList.getComments()).hasSizeGreaterThan(0);
        assertThat(postDetail.getCommentCount()).isGreaterThan(0);

        System.out.println("🎯 완전한 댓글 시나리오 테스트 완료! 🎉");
    }

    @Test
    @DisplayName("🔀 댓글 정렬 및 페이징 시나리오 테스트")
    void commentSortingAndPagingScenario() throws Exception {
        System.out.println("\n🔀 댓글 정렬 및 페이징 시나리오 테스트...");

        // 준비 단계
        TestUserHelper.TestUser postAuthor = testUserHelper.createFounderUser();
        TestUserHelper.TestUser[] commenters = new TestUserHelper.TestUser[5];
        for (int i = 0; i < 5; i++) {
            commenters[i] = (i % 2 == 0) ? testUserHelper.createFounderUser() : testUserHelper.createInhabitantUser();
        }

        Long postId = createTestPost(postAuthor, "댓글 테스트용 게시글", "OTHERS");

        System.out.println("📝 테스트 환경 준비 완료");

        // ==================== 다양한 댓글 작성 ====================
        System.out.println("\n[대량 댓글 작성] 5명이 각각 댓글 작성...");
        String[] comments = {
            "첫 번째 댓글입니다! 🎉",
            "두 번째 댓글이에요~ 좋은 글이네요!",
            "세 번째 댓글. 정말 유용한 정보 감사합니다 👍",
            "네 번째 댓글입니다. 질문이 있는데요...",
            "다섯 번째이자 마지막 댓글! 모두 좋은 하루 되세요 😊"
        };

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/community/freeboard/{freeboardId}/comments", postId)
                            .header("Authorization", commenters[i].getAuthHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"content\": \"" + comments[i] + "\"}"))
                    .andExpect(status().isCreated());

            // 댓글 작성 간격을 위해 잠시 대기
            Thread.sleep(100);
        }

        System.out.println("✅ 5개 댓글 작성 완료");

        // ==================== 최신순 정렬 테스트 ====================
        System.out.println("\n[정렬 테스트] LATEST 정렬 확인...");
        MvcResult latestResult = mockMvc.perform(get("/community/freeboard/{freeboardId}/comments", postId)
                        .param("sort", "LATEST")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments").isArray())
                .andReturn();

        FreeboardCommentCursorResponse latestComments = objectMapper.readValue(
            latestResult.getResponse().getContentAsString(), FreeboardCommentCursorResponse.class);

        System.out.println("📊 최신순 정렬 결과:");
        latestComments.getComments().forEach(comment -> {
            System.out.println("  - " + comment.getContent());
        });

        // ==================== 오래된순 정렬 테스트 ====================
        System.out.println("\n[정렬 테스트] OLDEST 정렬 확인...");
        mockMvc.perform(get("/community/freeboard/{freeboardId}/comments", postId)
                        .param("sort", "OLDEST")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments").isArray());

        // ==================== 페이징 테스트 ====================
        System.out.println("\n[페이징 테스트] 페이지 크기별 조회...");

        // 2개씩 페이징
        System.out.println("🔍 댓글 페이징 테스트 시작: postId=" + postId);

        MvcResult page1Result;
        try {
            page1Result = mockMvc.perform(get("/community/freeboard/{freeboardId}/comments", postId)
                            .param("sort", "LATEST")
                            .param("size", "2"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.comments").isArray())
                    .andExpect(jsonPath("$.hasNext").exists())
                    .andReturn();
        } catch (Exception e) {
            System.out.println("🚨 댓글 페이징 API 실패: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }

        FreeboardCommentCursorResponse page1 = objectMapper.readValue(
            page1Result.getResponse().getContentAsString(), FreeboardCommentCursorResponse.class);

        System.out.println("📄 첫 페이지 결과:");
        System.out.println("  - 댓글 수: " + page1.getComments().size());
        System.out.println("  - 다음 페이지 존재: " + page1.isHasNext());

        if (page1.isHasNext() && page1.getNextCursor() != null) {
            // 다음 페이지 조회
            mockMvc.perform(get("/community/freeboard/{freeboardId}/comments", postId)
                            .param("sort", "LATEST")
                            .param("size", "2")
                            .param("cursor", page1.getNextCursor()))
                    .andDo(print())
                    .andExpect(status().isOk());
        }

        // ==================== 검증 ====================
        assertThat(latestComments.getComments()).hasSize(5);
        assertThat(page1.getComments()).hasSize(2);

        System.out.println("🎯 댓글 정렬 및 페이징 테스트 완료! 🎉");
    }

    @Test
    @DisplayName("🗑️ 댓글 삭제 시나리오 상세 테스트")
    void commentDeletionDetailScenario() throws Exception {
        System.out.println("\n🗑️ 댓글 삭제 시나리오 상세 테스트...");

        // 준비
        TestUserHelper.TestUser postAuthor = testUserHelper.createFounderUser();
        TestUserHelper.TestUser commenter = testUserHelper.createInhabitantUser();
        TestUserHelper.TestUser replier1 = testUserHelper.createFounderUser();
        TestUserHelper.TestUser replier2 = testUserHelper.createInhabitantUser();

        Long postId = createTestPost(postAuthor, "댓글 삭제 테스트용 게시글", "OTHERS");

        // 부모 댓글 작성
        MvcResult parentResult = mockMvc.perform(post("/community/freeboard/{freeboardId}/comments", postId)
                        .header("Authorization", commenter.getAuthHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"부모 댓글입니다. 이 댓글에 대댓글들이 달릴 예정이에요.\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        Long parentCommentId = objectMapper.readValue(
            parentResult.getResponse().getContentAsString(),
            FreeboardCommentCreateResponse.class).getCommentId();

        // 대댓글들 작성
        mockMvc.perform(post("/community/freeboard/{freeboardId}/comments", postId)
                        .header("Authorization", replier1.getAuthHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"첫 번째 대댓글이에요!\", \"parentCommentId\": " + parentCommentId + "}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/community/freeboard/{freeboardId}/comments", postId)
                        .header("Authorization", replier2.getAuthHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"두 번째 대댓글입니다!\", \"parentCommentId\": " + parentCommentId + "}"))
                .andExpect(status().isCreated());

        System.out.println("📝 댓글 구조 준비 완료 (부모 댓글 1개, 대댓글 2개)");

        // ==================== 대댓글이 있는 부모 댓글 삭제 ====================
        System.out.println("\n[부모 댓글 삭제] 대댓글이 있는 부모 댓글 삭제...");
        mockMvc.perform(delete("/community/freeboard/{freeboardId}/comments/{commentId}", postId, parentCommentId)
                        .header("Authorization", commenter.getAuthHeader()))
                .andDo(print())
                .andExpect(status().isNoContent());

        // ==================== 삭제 후 상태 확인 ====================
        System.out.println("\n[삭제 후 확인] 댓글 목록에서 삭제 상태 확인...");
        MvcResult afterDeleteResult = mockMvc.perform(get("/community/freeboard/{freeboardId}/comments", postId)
                        .param("sort", "LATEST"))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        FreeboardCommentCursorResponse afterDelete = objectMapper.readValue(
            afterDeleteResult.getResponse().getContentAsString(), FreeboardCommentCursorResponse.class);

        System.out.println("📊 삭제 후 댓글 상태:");
        afterDelete.getComments().forEach(comment -> {
            System.out.println("  - ID " + comment.getCommentId() + ": " +
                             (comment.isDeleted() ? "[삭제된 댓글입니다]" : comment.getContent()) +
                             " (깊이: " + comment.getDepth() + ")");
        });

        // ==================== 검증 ====================
        // 부모 댓글은 삭제되었지만 대댓글들은 여전히 조회 가능해야 함
        assertThat(afterDelete.getComments()).hasSizeGreaterThanOrEqualTo(3);

        // 삭제된 부모 댓글 찾기
        boolean foundDeletedParent = afterDelete.getComments().stream()
                .anyMatch(comment -> comment.getCommentId().equals(parentCommentId) && comment.isDeleted());
        assertThat(foundDeletedParent).isTrue();

        System.out.println("🎯 댓글 삭제 시나리오 테스트 완료! 🎉");
    }

    private Long createTestPost(TestUserHelper.TestUser user, String title, String category) throws Exception {
        MvcResult result = mockMvc.perform(multipart("/community/freeboard")
                        .param("title", title)
                        .param("content", "테스트용 게시글 내용입니다.")
                        .param("category", category)
                        .header("Authorization", user.getAuthHeader())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andReturn();

        FreeboardCreateResponse response = objectMapper.readValue(
            result.getResponse().getContentAsString(), FreeboardCreateResponse.class);
        return response.getPostId();
    }
}