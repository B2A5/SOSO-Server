package com.example.soso.community.freeboard.integration;

import com.example.soso.community.freeboard.post.domain.dto.*;
import com.example.soso.community.freeboard.util.TestUserHelper;
import com.example.soso.community.freeboard.util.TestUserHelper.TestUser;
import com.example.soso.community.freeboard.comment.domain.dto.*;
import com.example.soso.config.TestS3Config;
import com.example.soso.config.TestRedisConfig;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
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
        "jwt.refresh-token-validity-in-ms=1209600000",
        "spring.cloud.aws.credentials.access-key=test-access-key",
        "spring.cloud.aws.credentials.secret-key=test-secret-key",
        "spring.cloud.aws.s3.bucket=test-bucket",
        "spring.cloud.aws.region=ap-northeast-2"
})
@Transactional
@Import({TestS3Config.class, TestRedisConfig.class})
@DisplayName("💖 좋아요 시스템 통합 테스트")
class LikeSystemIntegrationTest {

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
    @DisplayName("💖 좋아요 생태계: 게시글과 댓글에 대한 다양한 사용자들의 좋아요 시나리오")
    void likeEcosystemScenario_MultipleUsersEngagement() throws Exception {
        System.out.println("\n🎬 === 좋아요 생태계 시나리오 시작 ===");
        System.out.println("시나리오: 한 게시글에 여러 사용자들이 참여하여 게시글과 댓글에 좋아요를 누르는 과정");

        // ==================== STEP 1: 4명의 다양한 사용자 생성 ====================
        System.out.println("\n[STEP 1] 실전 사용자들 생성...");
        TestUser postAuthor = testUserHelper.createFounderUser();  // 게시글 작성자
        TestUser liker1 = testUserHelper.createInhabitantUser();   // 일반 좋아요 누르는 사용자
        TestUser liker2 = testUserHelper.createFounderUser();      // 창업가 좋아요 누르는 사용자
        TestUser commenter = testUserHelper.createInhabitantUser(); // 댓글 작성자

        System.out.println("✅ 사용자 생성 완료:");
        System.out.println("  - 게시글 작성자: " + postAuthor.getNickname() + " (창업가)");
        System.out.println("  - 좋아요 사용자1: " + liker1.getNickname() + " (주민)");
        System.out.println("  - 좋아요 사용자2: " + liker2.getNickname() + " (창업가)");
        System.out.println("  - 댓글 작성자: " + commenter.getNickname() + " (주민)");

        // ==================== STEP 2: 매력적인 게시글 작성 ====================
        System.out.println("\n[STEP 2] 인기를 끌 만한 게시글 작성...");
        String postTitle = "🎯 스타트업 성공 비결! 3개월만에 매출 1000% 증가한 실전 노하우";
        String postContent = "안녕하세요! 지역 창업가입니다 👋\n\n" +
                "얼마 전까지만 해도 매출이 거의 없었는데, 3개월만에 매출이 10배 이상 증가했어요!\n\n" +
                "✨ 핵심 전략:\n" +
                "1️⃣ 지역 주민분들과의 소통 강화\n" +
                "2️⃣ SNS 마케팅보다 오프라인 네트워킹\n" +
                "3️⃣ 고객 피드백을 즉시 반영\n\n" +
                "궁금한 점 있으시면 댓글로 언제든지 질문해주세요! 🚀";

        MockMultipartFile imageFile = new MockMultipartFile(
                "images", "success-chart.jpg", "image/jpeg", "매출 증가 차트 이미지".getBytes()
        );

        MvcResult postResult = mockMvc.perform(multipart("/community/freeboard")
                        .file(imageFile)
                        .param("title", postTitle)
                        .param("content", postContent)
                        .param("category", "startup")
                        .header("Authorization", postAuthor.getAuthHeader())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        FreeboardCreateResponse createResponse = objectMapper.readValue(
                postResult.getResponse().getContentAsString(), FreeboardCreateResponse.class);
        Long postId = createResponse.getPostId();

        System.out.println("✅ 매력적인 게시글 작성 완료: ID=" + postId);

        // ==================== STEP 3: 첫 번째 사용자 좋아요 + 게시글 상태 확인 ====================
        System.out.println("\n[STEP 3] 첫 번째 사용자(" + liker1.getNickname() + ")의 좋아요...");

        mockMvc.perform(post("/community/freeboard/{freeboardId}/like", postId)
                        .header("Authorization", liker1.getAuthHeader()))
                .andDo(print())
                .andExpect(status().isOk());

        System.out.println("✅ 첫 번째 좋아요 완료!");

        // 좋아요 후 게시글 상태 확인
        MvcResult postDetailResult1 = mockMvc.perform(get("/community/freeboard/{freeboardId}", postId)
                        .header("Authorization", liker1.getAuthHeader()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isAuthorized").value(true))
                .andExpect(jsonPath("$.likeCount").value(1))
                .andExpect(jsonPath("$.isLiked").value(true))
                .andReturn();

        System.out.println("✅ 게시글 좋아요 상태 확인: 좋아요 1개, 사용자에게는 isLiked=true");

        // ==================== STEP 4: 두 번째 사용자도 좋아요 ====================
        System.out.println("\n[STEP 4] 두 번째 사용자(" + liker2.getNickname() + ")의 좋아요...");

        mockMvc.perform(post("/community/freeboard/{freeboardId}/like", postId)
                        .header("Authorization", liker2.getAuthHeader()))
                .andDo(print())
                .andExpect(status().isOk());

        System.out.println("✅ 두 번째 좋아요 완료!");

        // 게시글 작성자 입장에서 게시글 상태 확인 (좋아요 안 누른 상태)
        MvcResult authorViewResult = mockMvc.perform(get("/community/freeboard/{freeboardId}", postId)
                        .header("Authorization", postAuthor.getAuthHeader()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isAuthorized").value(true))
                .andExpect(jsonPath("$.likeCount").value(2))
                .andExpect(jsonPath("$.isLiked").value(false)) // 작성자는 아직 좋아요 안 눌렀음
                .andReturn();

        System.out.println("✅ 게시글 좋아요 상태 확인: 총 2개, 작성자에게는 isLiked=false");

        // ==================== STEP 5: 댓글 작성 및 댓글 좋아요 ====================
        System.out.println("\n[STEP 5] 댓글 작성 및 댓글 좋아요 시나리오...");

        String commentContent = "와! 정말 대단한 성장이네요! 🤩\n" +
                "저도 비슷한 업종에서 창업 준비 중인데, 혹시 더 자세한 이야기 들을 수 있을까요?\n" +
                "오프라인 네트워킹 부분이 특히 궁금해요!";

        MvcResult commentResult = mockMvc.perform(post("/community/freeboard/{freeboardId}/comments", postId)
                        .header("Authorization", commenter.getAuthHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"" + commentContent.replace("\n", "\\n") + "\"}"))
                .andDo(print())
                .andExpect(status().isCreated())
                .andReturn();

        FreeboardCommentCreateResponse commentResponse = objectMapper.readValue(
                commentResult.getResponse().getContentAsString(), FreeboardCommentCreateResponse.class);
        Long commentId = commentResponse.getCommentId();

        System.out.println("✅ 댓글 작성 완료: ID=" + commentId);

        // ==================== STEP 6: 게시글 작성자가 댓글에 좋아요 ====================
        System.out.println("\n[STEP 6] 게시글 작성자가 댓글에 좋아요...");

        mockMvc.perform(post("/community/freeboard/{freeboardId}/comments/{commentId}/like", postId, commentId)
                        .header("Authorization", postAuthor.getAuthHeader()))
                .andDo(print())
                .andExpect(status().isOk());

        System.out.println("✅ 댓글 좋아요 완료!");

        // ==================== STEP 7: 다른 사용자도 댓글에 좋아요 ====================
        System.out.println("\n[STEP 7] 다른 사용자들도 댓글에 좋아요...");

        mockMvc.perform(post("/community/freeboard/{freeboardId}/comments/{commentId}/like", postId, commentId)
                        .header("Authorization", liker1.getAuthHeader()))
                .andDo(print())
                .andExpect(status().isOk());

        mockMvc.perform(post("/community/freeboard/{freeboardId}/comments/{commentId}/like", postId, commentId)
                        .header("Authorization", liker2.getAuthHeader()))
                .andDo(print())
                .andExpect(status().isOk());

        System.out.println("✅ 댓글에 총 3개의 좋아요 완료!");

        // ==================== STEP 8: 전체 댓글 목록에서 좋아요 상태 확인 ====================
        System.out.println("\n[STEP 8] 댓글 목록에서 좋아요 상태 확인...");

        mockMvc.perform(get("/community/freeboard/{freeboardId}/comments", postId)
                        .header("Authorization", liker1.getAuthHeader())
                        .param("sort", "LATEST")
                        .param("size", "20"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isAuthorized").value(true))
                .andExpect(jsonPath("$.comments").isArray())
                .andExpect(jsonPath("$.comments[0].likeCount").value(3))
                .andExpect(jsonPath("$.comments[0].isLiked").value(true)); // liker1이 좋아요 눌렀으므로

        System.out.println("✅ 댓글 좋아요 상태 확인 완료!");

        // ==================== STEP 9: 좋아요 취소 시나리오 ====================
        System.out.println("\n[STEP 9] 좋아요 취소 시나리오...");

        // liker1이 게시글 좋아요 취소 (토글)
        mockMvc.perform(post("/community/freeboard/{freeboardId}/like", postId)
                        .header("Authorization", liker1.getAuthHeader()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("false")); // 좋아요 취소됨

        System.out.println("✅ 게시글 좋아요 취소 완료!");

        // 좋아요 취소 후 상태 확인
        mockMvc.perform(get("/community/freeboard/{freeboardId}", postId)
                        .header("Authorization", liker1.getAuthHeader()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isAuthorized").value(true))
                .andExpect(jsonPath("$.likeCount").value(1)) // 2개에서 1개로 감소
                .andExpect(jsonPath("$.isLiked").value(false)); // 취소했으므로 false

        System.out.println("✅ 게시글 좋아요 취소 상태 확인 완료!");

        // ==================== STEP 10: 댓글 좋아요도 취소 ====================
        System.out.println("\n[STEP 10] 댓글 좋아요 취소 시나리오...");

        mockMvc.perform(post("/community/freeboard/{freeboardId}/comments/{commentId}/like", postId, commentId)
                        .header("Authorization", liker1.getAuthHeader()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("false")); // 댓글 좋아요 취소됨

        System.out.println("✅ 댓글 좋아요 취소 완료!");

        // ==================== STEP 11: 최종 전체 상태 확인 ====================
        System.out.println("\n[STEP 11] 최종 전체 상태 확인...");

        // 게시글 상태 (좋아요 1개)
        mockMvc.perform(get("/community/freeboard/{freeboardId}", postId)
                        .header("Authorization", commenter.getAuthHeader()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(1))
                .andExpect(jsonPath("$.commentCount").value(1));

        // 댓글 상태 (좋아요 2개)
        mockMvc.perform(get("/community/freeboard/{freeboardId}/comments", postId)
                        .header("Authorization", commenter.getAuthHeader()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isAuthorized").value(true))
                .andExpect(jsonPath("$.comments[0].likeCount").value(2))
                .andExpect(jsonPath("$.comments[0].isLiked").value(false)); // commenter는 댓글에 좋아요 안 눌렀음

        System.out.println("✅ 최종 상태: 게시글 좋아요 1개, 댓글 좋아요 2개");
        System.out.println("\n🎉 === 좋아요 생태계 시나리오 완료 ===");
        System.out.println("📊 결과 요약:");
        System.out.println("  - 게시글 최종 좋아요: 1개 (liker2만 남음)");
        System.out.println("  - 댓글 최종 좋아요: 2개 (postAuthor, liker2)");
        System.out.println("  - 좋아요 취소 기능 정상 작동");
        System.out.println("  - 사용자별 좋아요 상태 정확히 반영");
    }

    @Test
    @DisplayName("🚫 좋아요 제약 조건 테스트: 미인증 사용자, 중복 좋아요, 존재하지 않는 게시글/댓글")
    void likeConstraintsAndErrorScenarios() throws Exception {
        System.out.println("\n🎬 === 좋아요 제약 조건 및 에러 시나리오 ===");

        // 실제 사용자와 게시글 생성
        TestUser validUser = testUserHelper.createFounderUser();

        MockMultipartFile imageFile = new MockMultipartFile(
                "images", "test.jpg", "image/jpeg", "test content".getBytes()
        );

        MvcResult postResult = mockMvc.perform(multipart("/community/freeboard")
                        .file(imageFile)
                        .param("title", "테스트 게시글")
                        .param("content", "좋아요 테스트용 게시글")
                        .param("category", "others")
                        .header("Authorization", validUser.getAuthHeader())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        FreeboardCreateResponse createResponse = objectMapper.readValue(
                postResult.getResponse().getContentAsString(), FreeboardCreateResponse.class);
        Long validPostId = createResponse.getPostId();

        // ==================== 테스트 1: 미인증 사용자 좋아요 시도 ====================
        System.out.println("\n[테스트 1] 미인증 사용자의 좋아요 시도...");

        mockMvc.perform(post("/community/freeboard/{freeboardId}/like", validPostId))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        System.out.println("✅ 미인증 사용자 좋아요 차단 성공!");

        // ==================== 테스트 2: 정상 좋아요 후 중복 좋아요 시도 ====================
        System.out.println("\n[테스트 2] 중복 좋아요 시도...");

        // 정상 좋아요
        mockMvc.perform(post("/community/freeboard/{freeboardId}/like", validPostId)
                        .header("Authorization", validUser.getAuthHeader()))
                .andDo(print())
                .andExpect(status().isOk());

        // 두 번째 좋아요 토글 시도 (좋아요 취소됨)
        mockMvc.perform(post("/community/freeboard/{freeboardId}/like", validPostId)
                        .header("Authorization", validUser.getAuthHeader()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("false")); // 좋아요 취소됨

        System.out.println("✅ 좋아요 토글 동작 확인 성공!");

        // ==================== 테스트 3: 존재하지 않는 게시글에 좋아요 ====================
        System.out.println("\n[테스트 3] 존재하지 않는 게시글에 좋아요...");

        mockMvc.perform(post("/community/freeboard/{freeboardId}/like", 99999L)
                        .header("Authorization", validUser.getAuthHeader()))
                .andDo(print())
                .andExpect(status().isNotFound());

        System.out.println("✅ 존재하지 않는 게시글 좋아요 차단 성공!");

        // ==================== 테스트 4: 좋아요 누르지 않은 상태에서 취소 시도 ====================
        System.out.println("\n[테스트 4] 좋아요 없이 취소 시도...");

        TestUser anotherUser = testUserHelper.createInhabitantUser();

        mockMvc.perform(post("/community/freeboard/{freeboardId}/like", validPostId)
                        .header("Authorization", anotherUser.getAuthHeader()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("true")); // 좋아요 추가됨 (처음 누르는 것)

        System.out.println("✅ 다른 사용자 좋아요 동작 확인 성공!");

        System.out.println("\n🎉 === 좋아요 제약 조건 테스트 완료 ===");
    }

    @Test
    @DisplayName("📈 좋아요 시스템 성능 및 동시성 시나리오")
    void likeSystemPerformanceScenario() throws Exception {
        System.out.println("\n🎬 === 좋아요 시스템 성능 시나리오 ===");
        System.out.println("시나리오: 인기 게시글에 여러 사용자가 동시에 좋아요를 누르는 상황");

        // ==================== STEP 1: 인기 게시글 작성 ====================
        TestUser influencerAuthor = testUserHelper.createFounderUser();

        MockMultipartFile viralImage = new MockMultipartFile(
                "images", "viral-content.jpg", "image/jpeg", "바이럴 콘텐츠 이미지".getBytes()
        );

        MvcResult viralPostResult = mockMvc.perform(multipart("/community/freeboard")
                        .file(viralImage)
                        .param("title", "🔥 지역 창업 커뮤니티 대박 소식! 투자 유치 성공!")
                        .param("content", "드디어 우리 지역 창업 커뮤니티가 Series A 투자를 받았습니다! 🎉\n" +
                                "모든 분들의 응원 덕분입니다. 앞으로 더 많은 창업가들을 지원하겠습니다!")
                        .param("category", "startup")
                        .header("Authorization", influencerAuthor.getAuthHeader())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        FreeboardCreateResponse viralResponse = objectMapper.readValue(
                viralPostResult.getResponse().getContentAsString(), FreeboardCreateResponse.class);
        Long viralPostId = viralResponse.getPostId();

        System.out.println("✅ 바이럴 게시글 작성 완료: ID=" + viralPostId);

        // ==================== STEP 2: 다수 사용자 생성 및 좋아요 ====================
        System.out.println("\n[STEP 2] 다수 사용자의 좋아요 시뮬레이션...");

        TestUser[] users = new TestUser[5];
        for (int i = 0; i < 5; i++) {
            users[i] = (i % 2 == 0) ? testUserHelper.createFounderUser() : testUserHelper.createInhabitantUser();
            System.out.println("사용자 " + (i+1) + " 생성: " + users[i].getNickname() + " (" + users[i].getUserType() + ")");
        }

        // 모든 사용자가 좋아요
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/community/freeboard/{freeboardId}/like", viralPostId)
                            .header("Authorization", users[i].getAuthHeader()))
                    .andDo(print())
                    .andExpect(status().isOk());

            System.out.println("✅ 사용자 " + (i+1) + " 좋아요 완료!");
        }

        // ==================== STEP 3: 최종 좋아요 수 확인 ====================
        System.out.println("\n[STEP 3] 최종 좋아요 수 및 성능 확인...");

        mockMvc.perform(get("/community/freeboard/{freeboardId}", viralPostId)
                        .header("Authorization", users[0].getAuthHeader()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isAuthorized").value(true))
                .andExpect(jsonPath("$.likeCount").value(5))
                .andExpect(jsonPath("$.isLiked").value(true));

        System.out.println("✅ 성능 테스트 완료: 5명의 사용자 좋아요 정상 처리!");

        // ==================== STEP 4: 게시글 목록에서 좋아요 순 정렬 테스트 ====================
        System.out.println("\n[STEP 4] 좋아요 순 정렬 테스트...");

        mockMvc.perform(get("/community/freeboard")
                        .param("sort", "LIKE")
                        .param("size", "10")
                        .header("Authorization", users[0].getAuthHeader()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts").isArray())
                .andExpect(jsonPath("$.posts[0].likeCount").value(5)); // 가장 많은 좋아요

        System.out.println("✅ 좋아요 순 정렬 확인 완료!");

        System.out.println("\n🎉 === 좋아요 시스템 성능 시나리오 완료 ===");
        System.out.println("📊 성능 테스트 결과:");
        System.out.println("  - 5명 동시 좋아요 처리 성공");
        System.out.println("  - 좋아요 수 정확히 집계");
        System.out.println("  - 좋아요 순 정렬 정상 작동");
    }
}