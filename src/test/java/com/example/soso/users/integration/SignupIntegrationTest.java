package com.example.soso.users.integration;

import com.example.soso.users.domain.entity.*;
import com.example.soso.users.domain.dto.SignupSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
@DisplayName("회원가입 플로우 통합 테스트")
class SignupIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        session = new MockHttpSession();
        // 세션에 SignupSession 객체 초기화
        SignupSession signupSession = new SignupSession();
        signupSession.setUsername("testuser");
        signupSession.setEmail("test@example.com");
        signupSession.setProfileImageUrl("https://example.com/profile.jpg");
        session.setAttribute("signup", signupSession);
    }

    @Test
    @DisplayName("INHABITANT 유저 완전한 회원가입 플로우 테스트")
    void inhabitantCompleteSignupFlow() throws Exception {
        // 1. 유저타입 설정
        mockMvc.perform(post("/signup/user-type")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "userType": "INHABITANT"
                                }
                                """))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("REGION"));

        // 2. 지역 설정
        mockMvc.perform(post("/signup/region")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "regionId": "SEOUL"
                                }
                                """))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("AGE"));

        // 3. 연령대 설정
        mockMvc.perform(post("/signup/age-range")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "ageRange": "TWENTIES"
                                }
                                """))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("GENDER"));

        // 4. 성별 설정
        mockMvc.perform(post("/signup/gender")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "gender": "MALE"
                                }
                                """))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("NINAME"));
    }

    @Test
    @DisplayName("FOUNDER 유저 완전한 회원가입 플로우 테스트")
    void founderCompleteSignupFlow() throws Exception {
        // 1. 유저타입 설정
        mockMvc.perform(post("/signup/user-type")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "userType": "FOUNDER"
                                }
                                """))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("REGION"));

        // 2. 지역 설정
        mockMvc.perform(post("/signup/region")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "regionId": "SEOUL"
                                }
                                """))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("AGE"));

        // 3. 연령대 설정
        mockMvc.perform(post("/signup/age-range")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "ageRange": "THIRTIES"
                                }
                                """))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("GENDER"));

        // 4. 성별 설정
        mockMvc.perform(post("/signup/gender")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "gender": "FEMALE"
                                }
                                """))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("INTERESTS"));

        // 5. 관심 업종 설정
        mockMvc.perform(post("/signup/interests")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "interests": ["MANUFACTURING", "ACCOMMODATION_FOOD"]
                                }
                                """))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("BUDGET"));

        // 6. 예산 설정
        mockMvc.perform(post("/signup/budget")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "budget": "UNDER_50"
                                }
                                """))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("STARTUP"));
    }

    @Test
    @DisplayName("역방향 네비게이션 테스트 - 이전 단계로 돌아가기")
    void backwardNavigationTest() throws Exception {
        // 초기 단계들 완료
        mockMvc.perform(post("/signup/user-type")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userType\": \"INHABITANT\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/signup/region")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"regionId\": \"SEOUL\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/signup/age-range")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ageRange\": \"TWENTIES\"}"))
                .andExpect(status().isOk());

        // 새로운 strict validation으로 인해 역방향 네비게이션은 불가능
        // 현재 GENDER 단계에서 REGION으로 역행 시도 - 실패해야 함
        mockMvc.perform(post("/signup/region")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "regionId": "BUSAN"
                                }
                                """))
                .andDo(print())
                .andExpect(status().isUnauthorized()); // 단계 검증 실패로 401 반환
    }

    @Test
    @DisplayName("잘못된 단계 순서로 요청시 실패 테스트")
    void invalidStepOrderTest() throws Exception {
        // 유저타입만 설정하고 바로 연령대 설정 시도 (REGION 건너뛰기)
        mockMvc.perform(post("/signup/user-type")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userType\": \"INHABITANT\"}"))
                .andExpect(status().isOk());

        // REGION을 건너뛰고 GENDER 설정 시도 (2단계 건너뛰기) - 실패해야 함
        mockMvc.perform(post("/signup/gender")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gender\": \"MALE\"}"))
                .andDo(print())
                .andExpect(status().isUnauthorized()); // STEPS_NOT_TYPE는 401 반환
    }

    @Test
    @DisplayName("세션 없이 회원가입 시도시 실패 테스트")
    void signupWithoutSessionTest() throws Exception {
        MockHttpSession emptySession = new MockHttpSession();

        mockMvc.perform(post("/signup/region")
                        .session(emptySession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"regionId\": \"SEOUL\"}"))
                .andDo(print())
                .andExpect(status().isUnauthorized()); // SESSION_NOT_VALID는 401 반환
    }
}