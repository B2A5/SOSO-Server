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
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:authtest;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE",
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
@DisplayName("🔐 인증/미인증 권한 시나리오 테스트")
class AuthenticationPermissionTest {

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
    @DisplayName("🔓 미인증 사용자의 접근 권한 매트릭스 테스트")
    void unauthenticatedUserAccessMatrix() throws Exception {
        System.out.println("\n🔓 미인증 사용자 접근 권한 매트릭스 테스트 시작...");

        // 테스트용 게시글 생성 (인증된 사용자로)
        TestUserHelper.TestUser author = testUserHelper.createFounderUser();
        Long postId = createTestPost(author, "미인증 접근 테스트용 게시글", "STARTUP");

        System.out.println("📝 테스트용 게시글 준비 완료: ID=" + postId);

        // ==================== 미인증 상태에서 허용되는 작업들 ====================
        System.out.println("\n✅ [허용] 미인증 사용자가 접근 가능한 작업들:");

        // 1. 게시글 목록 조회 - 성공해야 함
        System.out.println("  1. 게시글 목록 조회...");
        mockMvc.perform(get("/community/freeboard")
                        .param("sort", "LATEST")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts").isArray())
                .andExpect(jsonPath("$.totalCount").exists());

        // 2. 게시글 상세 조회 - 성공해야 함 (비인증이므로 isEditable/isDeletable/isLiked는 null, isAuthorized는 false)
        System.out.println("  2. 게시글 상세 조회...");
        MvcResult detailResult = mockMvc.perform(get("/community/freeboard/{freeboardId}", postId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(postId))
                .andExpect(jsonPath("$.isAuthorized").value(false))
                .andExpect(jsonPath("$.isEditable").value(nullValue()))
                .andExpect(jsonPath("$.isDeletable").value(nullValue()))
                .andExpect(jsonPath("$.isLiked").value(nullValue()))
                .andExpect(jsonPath("$.author.userType").value("FOUNDER"))
                .andExpect(jsonPath("$.author.address").exists())
                .andReturn();

        FreeboardDetailResponse response = objectMapper.readValue(
            detailResult.getResponse().getContentAsString(), FreeboardDetailResponse.class);

        // 3. 카테고리별 게시글 조회 - 성공해야 함
        System.out.println("  3. 카테고리별 게시글 조회...");
        mockMvc.perform(get("/community/freeboard")
                        .param("category", "STARTUP")
                        .param("sort", "LATEST"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts").isArray());

        // 4. 댓글 목록 조회 - 성공해야 함
        System.out.println("  4. 댓글 목록 조회...");
        mockMvc.perform(get("/community/freeboard/{freeboardId}/comments", postId)
                        .param("sort", "LATEST"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments").isArray());

        System.out.println("✅ 미인증 사용자 허용 작업들 모두 통과!");

        // ==================== 미인증 상태에서 차단되는 작업들 ====================
        System.out.println("\n❌ [차단] 미인증 사용자가 접근 불가능한 작업들:");

        // 1. 게시글 작성 - 401 Unauthorized
        System.out.println("  1. 게시글 작성 차단 확인...");
        mockMvc.perform(multipart("/community/freeboard")
                        .param("title", "미인증 작성 시도")
                        .param("content", "미인증 상태에서 작성 시도")
                        .param("category", "OTHERS")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        // 2. 게시글 수정 - 401 Unauthorized
        System.out.println("  2. 게시글 수정 차단 확인...");
        mockMvc.perform(multipart("/community/freeboard/{freeboardId}", postId)
                        .param("title", "미인증 수정 시도")
                        .param("content", "미인증 상태에서 수정 시도")
                        .param("category", "STARTUP")
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        }))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        // 3. 게시글 삭제 - 401 Unauthorized
        System.out.println("  3. 게시글 삭제 차단 확인...");
        mockMvc.perform(delete("/community/freeboard/{freeboardId}", postId))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        // 4. 댓글 작성 - 401 Unauthorized
        System.out.println("  4. 댓글 작성 차단 확인...");
        mockMvc.perform(post("/community/freeboard/{freeboardId}/comments", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"미인증 댓글 작성 시도\"}"))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        // 5. 좋아요 - 401 Unauthorized
        System.out.println("  5. 좋아요 차단 확인...");
        mockMvc.perform(post("/community/freeboard/{freeboardId}/like", postId))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        System.out.println("✅ 미인증 사용자 차단 작업들 모두 확인!");

        // ==================== 검증 ====================
        assertThat(response.isAuthorized()).isFalse();
        assertThat(response.getIsEditable()).isNull();
        assertThat(response.getIsDeletable()).isNull();
        assertThat(response.getIsLiked()).isNull();
        assertThat(response.getAuthor().getUserType().toString()).isEqualTo("FOUNDER");
        assertThat(response.getAuthor().getAddress()).isNotEmpty();

        System.out.println("🎯 미인증 사용자 접근 권한 매트릭스 테스트 완료! 🎉");
    }

    @Test
    @DisplayName("👤 인증된 사용자의 권한별 상세 테스트")
    void authenticatedUserPermissionDetails() throws Exception {
        System.out.println("\n👤 인증된 사용자 권한 상세 테스트 시작...");

        // 두 명의 사용자 생성
        TestUserHelper.TestUser originalAuthor = testUserHelper.createFounderUser();
        TestUserHelper.TestUser otherUser = testUserHelper.createInhabitantUser();

        System.out.println("👥 테스트 사용자 준비:");
        System.out.println("  - 원작성자: " + originalAuthor.getNickname() + " (FOUNDER)");
        System.out.println("  - 다른사용자: " + otherUser.getNickname() + " (INHABITANT)");

        // 게시글 생성
        Long postId = createTestPost(originalAuthor, "권한 테스트용 창업 아이템 아이디어", "STARTUP");

        // ==================== 원작성자 권한 테스트 ====================
        System.out.println("\n✅ [원작성자] 모든 권한이 있는 사용자 테스트:");

        // 1. 상세 조회 - isAuthorized=true, isEditable/isDeletable=true, isLiked=false
        System.out.println("  1. 원작성자의 게시글 조회...");
        MvcResult authorResult = mockMvc.perform(get("/community/freeboard/{freeboardId}", postId)
                        .header("Authorization", originalAuthor.getAuthHeader()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isAuthorized").value(true))
                .andExpect(jsonPath("$.isEditable").value(true))
                .andExpect(jsonPath("$.isDeletable").value(true))
                .andExpect(jsonPath("$.isLiked").value(false))
                .andExpect(jsonPath("$.author.userType").value("FOUNDER"))
                .andReturn();

        // 2. 수정 성공
        System.out.println("  2. 원작성자의 게시글 수정...");
        mockMvc.perform(multipart("/community/freeboard/{freeboardId}", postId)
                        .param("title", "수정된 창업 아이템 아이디어")
                        .param("content", "원작성자가 내용을 수정했습니다.")
                        .param("category", "STARTUP")
                        .header("Authorization", originalAuthor.getAuthHeader())
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        }))
                .andDo(print())
                .andExpect(status().isOk());

        System.out.println("✅ 원작성자 권한 확인 완료");

        // ==================== 다른 사용자 권한 테스트 ====================
        System.out.println("\n🚫 [다른사용자] 제한된 권한을 가진 사용자 테스트:");

        // 1. 상세 조회 - isAuthorized=true, isEditable/isDeletable=false, isLiked=false
        System.out.println("  1. 다른 사용자의 게시글 조회...");
        mockMvc.perform(get("/community/freeboard/{freeboardId}", postId)
                        .header("Authorization", otherUser.getAuthHeader()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isAuthorized").value(true))
                .andExpect(jsonPath("$.isEditable").value(false))
                .andExpect(jsonPath("$.isDeletable").value(false))
                .andExpect(jsonPath("$.isLiked").value(false))
                .andExpect(jsonPath("$.author.userType").value("FOUNDER"));

        // 2. 수정 시도 - 403 Forbidden
        System.out.println("  2. 다른 사용자의 수정 시도 (차단되어야 함)...");
        mockMvc.perform(multipart("/community/freeboard/{freeboardId}", postId)
                        .param("title", "다른 사용자의 수정 시도")
                        .param("content", "다른 사용자가 수정을 시도합니다.")
                        .param("category", "STARTUP")
                        .header("Authorization", otherUser.getAuthHeader())
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        }))
                .andDo(print())
                .andExpect(status().isForbidden());

        // 3. 삭제 시도 - 403 Forbidden
        System.out.println("  3. 다른 사용자의 삭제 시도 (차단되어야 함)...");
        mockMvc.perform(delete("/community/freeboard/{freeboardId}", postId)
                        .header("Authorization", otherUser.getAuthHeader()))
                .andDo(print())
                .andExpect(status().isForbidden());

        // 4. 하지만 댓글은 작성 가능 - 201 Created
        System.out.println("  4. 다른 사용자의 댓글 작성 (허용되어야 함)...");
        mockMvc.perform(post("/community/freeboard/{freeboardId}/comments", postId)
                        .header("Authorization", otherUser.getAuthHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"거주민 관점에서 좋은 아이디어인 것 같아요! 응원합니다 👍\"}"))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.commentId").exists());

        // 5. 좋아요도 가능 - 200 OK
        System.out.println("  5. 다른 사용자의 좋아요 (허용되어야 함)...");
        mockMvc.perform(post("/community/freeboard/{freeboardId}/like", postId)
                        .header("Authorization", otherUser.getAuthHeader()))
                .andDo(print())
                .andExpect(status().isOk());

        System.out.println("✅ 다른 사용자 권한 제한 확인 완료");

        // ==================== 최종 권한 상태 확인 ====================
        System.out.println("\n🔍 [최종확인] 게시글 현재 상태:");
        MvcResult finalResult = mockMvc.perform(get("/community/freeboard/{freeboardId}", postId)
                        .header("Authorization", originalAuthor.getAuthHeader()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentCount").value(1))
                .andExpect(jsonPath("$.likeCount").value(1))
                .andReturn();

        FreeboardDetailResponse finalResponse = objectMapper.readValue(
            finalResult.getResponse().getContentAsString(), FreeboardDetailResponse.class);

        System.out.println("📊 최종 상태:");
        System.out.println("  - 댓글 수: " + finalResponse.getCommentCount());
        System.out.println("  - 좋아요 수: " + finalResponse.getLikeCount());

        // ==================== 검증 ====================
        assertThat(finalResponse.isAuthorized()).isTrue();
        assertThat(finalResponse.getCommentCount()).isEqualTo(1);
        assertThat(finalResponse.getLikeCount()).isEqualTo(1);
        assertThat(finalResponse.getIsEditable()).isTrue(); // 원작성자 관점
        assertThat(finalResponse.getIsDeletable()).isTrue(); // 원작성자 관점
        assertThat(finalResponse.getIsLiked()).isFalse(); // 원작성자는 자신의 글에 좋아요 안 함

        System.out.println("🎯 인증된 사용자 권한 상세 테스트 완료! 🎉");
    }

    @Test
    @DisplayName("🔄 사용자 유형별 권한 상호작용 테스트")
    void userTypeInteractionTest() throws Exception {
        System.out.println("\n🔄 사용자 유형별 상호작용 테스트...");

        // 창업가와 거주민 생성
        TestUserHelper.TestUser founder = testUserHelper.createFounderUser();
        TestUserHelper.TestUser inhabitant = testUserHelper.createInhabitantUser();

        System.out.println("👥 사용자 준비:");
        System.out.println("  - 창업가: " + founder.getNickname());
        System.out.println("  - 거주민: " + inhabitant.getNickname());

        // 각자 게시글 작성
        Long founderPostId = createTestPost(founder, "창업가의 투자 유치 경험담", "STARTUP");
        Long inhabitantPostId = createTestPost(inhabitant, "동네 맛집 리스트 최종판", "RESTAURANT");

        System.out.println("📝 게시글 준비:");
        System.out.println("  - 창업가 게시글: " + founderPostId);
        System.out.println("  - 거주민 게시글: " + inhabitantPostId);

        // ==================== 교차 접근 테스트 ====================
        System.out.println("\n🔀 교차 접근 권한 테스트:");

        // 창업가 → 거주민 게시글 접근
        System.out.println("  1. 창업가가 거주민 게시글 조회...");
        mockMvc.perform(get("/community/freeboard/{freeboardId}", inhabitantPostId)
                        .header("Authorization", founder.getAuthHeader()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isAuthorized").value(true))
                .andExpect(jsonPath("$.isEditable").value(false))
                .andExpect(jsonPath("$.isDeletable").value(false))
                .andExpect(jsonPath("$.isLiked").value(false))
                .andExpect(jsonPath("$.author.userType").value("INHABITANT"));

        // 거주민 → 창업가 게시글 접근
        System.out.println("  2. 거주민이 창업가 게시글 조회...");
        mockMvc.perform(get("/community/freeboard/{freeboardId}", founderPostId)
                        .header("Authorization", inhabitant.getAuthHeader()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isAuthorized").value(true))
                .andExpect(jsonPath("$.isEditable").value(false))
                .andExpect(jsonPath("$.isDeletable").value(false))
                .andExpect(jsonPath("$.isLiked").value(false))
                .andExpect(jsonPath("$.author.userType").value("FOUNDER"));

        // ==================== 각자의 게시글에 대한 권한 확인 ====================
        System.out.println("\n✅ 각자 게시글에 대한 소유권 확인:");

        // 창업가 → 자신의 게시글
        mockMvc.perform(get("/community/freeboard/{freeboardId}", founderPostId)
                        .header("Authorization", founder.getAuthHeader()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isAuthorized").value(true))
                .andExpect(jsonPath("$.isEditable").value(true))
                .andExpect(jsonPath("$.isDeletable").value(true))
                .andExpect(jsonPath("$.isLiked").value(false));

        // 거주민 → 자신의 게시글
        mockMvc.perform(get("/community/freeboard/{freeboardId}", inhabitantPostId)
                        .header("Authorization", inhabitant.getAuthHeader()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isAuthorized").value(true))
                .andExpect(jsonPath("$.isEditable").value(true))
                .andExpect(jsonPath("$.isDeletable").value(true))
                .andExpect(jsonPath("$.isLiked").value(false));

        System.out.println("✅ 사용자 유형별 상호작용 테스트 완료! 🎉");
    }

    private Long createTestPost(TestUserHelper.TestUser user, String title, String category) throws Exception {
        MvcResult result = mockMvc.perform(multipart("/community/freeboard")
                        .param("title", title)
                        .param("content", "테스트용 게시글 내용입니다.")
                        .param("category", category)
                        .header("Authorization", user.getAuthHeader())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andReturn();

        FreeboardCreateResponse response = objectMapper.readValue(
            result.getResponse().getContentAsString(), FreeboardCreateResponse.class);
        return response.getPostId();
    }
}