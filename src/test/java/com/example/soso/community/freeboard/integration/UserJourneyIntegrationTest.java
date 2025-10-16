package com.example.soso.community.freeboard.integration;

import com.example.soso.community.freeboard.post.domain.dto.*;
import com.example.soso.community.freeboard.util.TestUserHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
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
@AutoConfigureWebMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.session.store-type=none",
        "jwt.secret-key=ThisIsAVerySecretKeyForTestingPurposesAndItShouldBeLongEnoughToMeetTheRequirements",
        "jwt.access-token-validity-in-ms=3600000",
        "jwt.refresh-token-validity-in-ms=1209600000"
})
@DisplayName("🚀 사용자 여정 통합 테스트 - 회원가입부터 게시글 CRUD까지")
class UserJourneyIntegrationTest {

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
    @DisplayName("📖 완전한 사용자 여정: 창업가가 맛집 정보를 공유하고 관리하는 전체 시나리오")
    void completeUserJourney_FounderSharesRestaurantInfo() throws Exception {
        // ==================== STEP 1: 회원가입 ====================
        System.out.println("\n🔥 [STEP 1] 예비창업가 회원가입 진행...");
        TestUserHelper.TestUser founder = testUserHelper.createFounderUser();

        System.out.println("✅ 회원가입 완료:");
        System.out.println("  - 사용자: " + founder.getNickname());
        System.out.println("  - 유형: " + founder.getUserType());
        System.out.println("  - 지역: " + founder.getLocation());
        System.out.println("  - JWT 토큰 발급 완료");

        // ==================== STEP 2: 게시글 작성 ====================
        System.out.println("\n📝 [STEP 2] 맛집 정보 게시글 작성...");
        String postTitle = "🍜 강남역 근처 진짜 맛있는 라멘집 발견!";
        String postContent = "예비창업자로 활동하면서 매일 이 근처를 돌아다니는데, " +
                "정말 숨은 보석 같은 라멘집을 발견했어요! " +
                "창업 준비하시는 분들, 맛있는 한 끼 드시고 에너지 충전하세요 💪\n\n" +
                "📍 위치: 강남역 3번 출구에서 도보 5분\n" +
                "💰 가격: 라멘 8,000원 (가성비 최고)\n" +
                "⏰ 운영시간: 11:00-22:00 (브레이크타임 없음)\n" +
                "👍 추천 메뉴: 돈코츠 라멘, 차슈멘";

        MvcResult createResult = mockMvc.perform(multipart("/community/freeboard")
                        .param("title", postTitle)
                        .param("content", postContent)
                        .param("category", "RESTAURANT")
                        .header("Authorization", founder.getAuthHeader())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").exists())
                .andReturn();

        String createResponseContent = createResult.getResponse().getContentAsString();
        FreeboardCreateResponse createResponse = objectMapper.readValue(createResponseContent, FreeboardCreateResponse.class);
        Long postId = createResponse.getPostId();

        System.out.println("✅ 게시글 작성 완료: ID=" + postId);

        // ==================== STEP 3: 게시글 목록에서 확인 ====================
        System.out.println("\n📋 [STEP 3] 게시글이 목록에 정상 노출되는지 확인...");
        MvcResult listResult = mockMvc.perform(get("/community/freeboard")
                        .param("sort", "LATEST")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts").isArray())
                .andExpect(jsonPath("$.posts[0].postId").value(postId))
                .andExpect(jsonPath("$.posts[0].title").value(postTitle))
                .andExpect(jsonPath("$.posts[0].author.userType").value("FOUNDER"))
                .andExpect(jsonPath("$.posts[0].author.nickname").value(founder.getNickname()))
                .andExpect(jsonPath("$.totalCount").exists())
                .andReturn();

        String listResponseContent = listResult.getResponse().getContentAsString();
        FreeboardCursorResponse listResponse = objectMapper.readValue(listResponseContent, FreeboardCursorResponse.class);

        System.out.println("✅ 목록 조회 확인:");
        System.out.println("  - 총 게시글 수: " + listResponse.getTotalCount());
        System.out.println("  - 첫 번째 게시글이 방금 작성한 글인지: " + listResponse.getPosts().get(0).getPostId().equals(postId));

        // ==================== STEP 4: 게시글 상세 조회 (작성자) ====================
        System.out.println("\n🔍 [STEP 4] 작성자 본인이 게시글 상세 조회...");
        MvcResult detailResult = mockMvc.perform(get("/community/freeboard/{freeboardId}", postId)
                        .header("Authorization", founder.getAuthHeader()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(postId))
                .andExpect(jsonPath("$.title").value(postTitle))
                .andExpect(jsonPath("$.content").value(postContent))
                .andExpect(jsonPath("$.category").value("restaurant"))
                .andExpect(jsonPath("$.author.userType").value("FOUNDER"))
                .andExpect(jsonPath("$.author.nickname").value(founder.getNickname()))
                .andExpect(jsonPath("$.author.address").exists())
                .andExpect(jsonPath("$.canEdit").value(true))
                .andExpect(jsonPath("$.canDelete").value(true))
                .andExpect(jsonPath("$.viewCount").exists())
                .andExpect(jsonPath("$.likeCount").value(0))
                .andExpect(jsonPath("$.commentCount").value(0))
                .andReturn();

        FreeboardDetailResponse detailResponse = objectMapper.readValue(
            detailResult.getResponse().getContentAsString(), FreeboardDetailResponse.class);

        System.out.println("✅ 상세 조회 확인 (작성자):");
        System.out.println("  - 편집 권한: " + detailResponse.isCanEdit());
        System.out.println("  - 삭제 권한: " + detailResponse.isCanDelete());
        System.out.println("  - 작성자 주소: " + detailResponse.getAuthor().getAddress());

        // ==================== STEP 5: 게시글 수정 ====================
        System.out.println("\n✏️ [STEP 5] 게시글 내용 업데이트...");
        String updatedTitle = postTitle + " [수정됨]";
        String updatedContent = postContent + "\n\n" +
                "=== 수정 내용 ===\n" +
                "📞 전화번호: 02-1234-5678\n" +
                "🚗 주차: 근처 공영주차장 이용 (도보 3분)\n" +
                "💡 팁: 점심시간에는 웨이팅이 있을 수 있어요!";

        mockMvc.perform(multipart("/community/freeboard/{freeboardId}", postId)
                        .param("title", updatedTitle)
                        .param("content", updatedContent)
                        .param("category", "RESTAURANT")
                        .header("Authorization", founder.getAuthHeader())
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        }))
                .andDo(print())
                .andExpect(status().isOk());

        System.out.println("✅ 게시글 수정 완료");

        // ==================== STEP 6: 수정된 내용 확인 ====================
        System.out.println("\n🔎 [STEP 6] 수정된 내용 확인...");
        mockMvc.perform(get("/community/freeboard/{freeboardId}", postId)
                        .header("Authorization", founder.getAuthHeader()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(updatedTitle))
                .andExpect(jsonPath("$.content").value(updatedContent))
                .andExpect(jsonPath("$.updatedAt").exists());

        System.out.println("✅ 수정 내용 확인 완료");

        // ==================== STEP 7: 다른 사용자 관점에서 조회 ====================
        System.out.println("\n👥 [STEP 7] 다른 사용자(거주민) 관점에서 게시글 조회...");
        TestUserHelper.TestUser inhabitant = testUserHelper.createInhabitantUser();

        MvcResult otherUserResult = mockMvc.perform(get("/community/freeboard/{freeboardId}", postId)
                        .header("Authorization", inhabitant.getAuthHeader()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(postId))
                .andExpect(jsonPath("$.title").value(updatedTitle))
                .andExpect(jsonPath("$.canEdit").value(false))
                .andExpect(jsonPath("$.canDelete").value(false))
                .andExpect(jsonPath("$.author.userType").value("FOUNDER"))
                .andReturn();

        System.out.println("✅ 다른 사용자 관점 확인:");
        System.out.println("  - " + inhabitant.getNickname() + "(" + inhabitant.getUserType() + ")는 편집/삭제 불가능");

        // ==================== STEP 8: 미인증 사용자 관점에서 조회 ====================
        System.out.println("\n🔓 [STEP 8] 미인증 사용자 관점에서 게시글 조회...");
        mockMvc.perform(get("/community/freeboard/{freeboardId}", postId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(postId))
                .andExpect(jsonPath("$.canEdit").value(false))
                .andExpect(jsonPath("$.canDelete").value(false))
                .andExpect(jsonPath("$.author.userType").value("FOUNDER"))
                .andExpect(jsonPath("$.author.address").exists());

        System.out.println("✅ 미인증 사용자 관점 확인: 조회는 가능하지만 편집/삭제 불가능");

        // ==================== STEP 9: 게시글 삭제 시도 (권한 없는 사용자) ====================
        System.out.println("\n❌ [STEP 9] 권한 없는 사용자의 삭제 시도 (실패해야 함)...");
        mockMvc.perform(delete("/community/freeboard/{freeboardId}", postId)
                        .header("Authorization", inhabitant.getAuthHeader()))
                .andDo(print())
                .andExpect(status().isForbidden());

        System.out.println("✅ 권한 없는 삭제 시도 차단 확인");

        // ==================== STEP 10: 게시글 삭제 (작성자) ====================
        System.out.println("\n🗑️ [STEP 10] 작성자가 게시글 삭제...");
        mockMvc.perform(delete("/community/freeboard/{freeboardId}", postId)
                        .header("Authorization", founder.getAuthHeader()))
                .andDo(print())
                .andExpect(status().isNoContent());

        System.out.println("✅ 게시글 삭제 완료");

        // ==================== STEP 11: 삭제된 게시글 조회 시도 ====================
        System.out.println("\n🔍 [STEP 11] 삭제된 게시글 조회 시도 (404 응답 확인)...");
        mockMvc.perform(get("/community/freeboard/{freeboardId}", postId))
                .andDo(print())
                .andExpect(status().isNotFound());

        System.out.println("✅ 삭제된 게시글 접근 차단 확인");

        // ==================== 최종 검증 ====================
        System.out.println("\n🎯 [최종 검증] 전체 사용자 여정 완료!");

        // 사용자 검증
        assertThat(founder.getUserType().toString()).isEqualTo("FOUNDER");
        assertThat(founder.getNickname()).isNotEmpty();
        assertThat(founder.getLocation()).isNotEmpty();

        // 게시글 생성 검증
        assertThat(postId).isNotNull();
        assertThat(postId).isPositive();

        // 권한 검증
        assertThat(detailResponse.isCanEdit()).isTrue();
        assertThat(detailResponse.isCanDelete()).isTrue();
        assertThat(detailResponse.getAuthor().getUserType().toString()).isEqualTo("FOUNDER");

        System.out.println("✅ 모든 검증 통과! 완전한 사용자 여정 시나리오 성공 🎉");
    }

    @Test
    @DisplayName("🏠 거주민 사용자의 생활 꿀팁 공유 시나리오")
    void inhabitantSharesLifeTips() throws Exception {
        // ==================== 거주민 회원가입 ====================
        System.out.println("\n🏠 거주민 회원가입 진행...");
        TestUserHelper.TestUser inhabitant = testUserHelper.createInhabitantUser();

        System.out.println("✅ 거주민 회원가입 완료: " + inhabitant.getNickname() + " (" + inhabitant.getUserType() + ")");

        // ==================== 생활 꿀팁 게시글 작성 ====================
        String tipTitle = "🏠 원룸 공간 활용 100% 꿀팁 대방출!";
        String tipContent = "좁은 원룸에서 5년째 살고 있는 거주민이 알려주는 진짜 꿀팁들이에요! " +
                "창업 준비하시는 분들도 많이 원룸에 사시니까 도움이 될 거예요 😊\n\n" +
                "1. 💡 조명으로 공간 분리하기\n" +
                "2. 📦 수직 공간 200% 활용법\n" +
                "3. 🛏️ 침대 밑 수납의 신세계\n" +
                "4. 🍳 초간단 원룸 요리 레시피\n" +
                "5. 💰 월세 절약하는 생활비 관리법";

        MvcResult createResult = mockMvc.perform(multipart("/community/freeboard")
                        .param("title", tipTitle)
                        .param("content", tipContent)
                        .param("category", "LIVING_CONVENIENCE")
                        .header("Authorization", inhabitant.getAuthHeader())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        FreeboardCreateResponse createResponse = objectMapper.readValue(
            createResult.getResponse().getContentAsString(), FreeboardCreateResponse.class);
        Long postId = createResponse.getPostId();

        System.out.println("✅ 생활 꿀팁 게시글 작성 완료: ID=" + postId);

        // ==================== 카테고리별 조회 테스트 ====================
        System.out.println("\n📂 카테고리별 게시글 조회 테스트...");
        mockMvc.perform(get("/community/freeboard")
                        .param("category", "LIVING_CONVENIENCE")
                        .param("sort", "LATEST")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts").isArray())
                .andExpect(jsonPath("$.posts[0].postId").value(postId))
                .andExpect(jsonPath("$.posts[0].category").value("living-convenience"))
                .andExpect(jsonPath("$.posts[0].author.userType").value("INHABITANT"));

        System.out.println("✅ 카테고리별 조회 확인 완료");

        // ==================== 검증 ====================
        assertThat(inhabitant.getUserType().toString()).isEqualTo("INHABITANT");
        assertThat(postId).isNotNull();

        System.out.println("🎉 거주민 시나리오 완료!");
    }
}