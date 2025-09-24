package com.example.soso.users.controller;

import com.example.soso.users.repository.UsersRepository;
import com.example.soso.users.domain.dto.SignupSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
@DisplayName("SignupController 통합 테스트")
class SignupControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;


    @Autowired
    private UsersRepository usersRepository;

    private MockMvc mockMvc;
    private MockHttpSession mockSession;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        mockSession = new MockHttpSession();
        initializeSession();
    }

    @Test
    @DisplayName("FOUNDER 전체 회원가입 플로우 통합 테스트")
    void completeFounderSignupFlow() throws Exception {
        // 1단계: 사용자 타입 설정
        mockMvc.perform(post("/signup/user-type")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userType": "FOUNDER"}
                                """))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("REGION"));

        // 2단계: 지역 설정
        mockMvc.perform(post("/signup/region")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"regionId": "11010"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("AGE"));

        // 3단계: 연령대 설정
        mockMvc.perform(post("/signup/age-range")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ageRange": "TWENTIES"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("GENDER"));

        // 4단계: 성별 설정
        mockMvc.perform(post("/signup/gender")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"gender": "MALE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("INTERESTS"));

        // 5단계: 관심업종 설정
        mockMvc.perform(post("/signup/interests")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"interests": ["MANUFACTURING", "ACCOMMODATION_FOOD"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("BUDGET"));

        // 6단계: 예산 설정
        mockMvc.perform(post("/signup/budget")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"budget": "THOUSANDS_3000_5000"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("STARTUP"));

        // 7단계: 창업 경험 설정
        mockMvc.perform(post("/signup/experience")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"experience": "NO"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("NINAME"));

        // 8단계: 닉네임 생성
        mockMvc.perform(post("/signup/nickname")
                        .session(mockSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isString())
                .andExpect(jsonPath("$").value(org.hamcrest.Matchers.endsWith("문어")));

        // 9단계: 회원가입 완료
        long userCountBefore = usersRepository.count();

        mockMvc.perform(post("/signup/complete")
                        .session(mockSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jwtAccessToken").isNotEmpty());

        // 데이터베이스에 사용자가 저장되었는지 확인
        long userCountAfter = usersRepository.count();
        assertThat(userCountAfter).isEqualTo(userCountBefore + 1);

        // 세션이 정리되었는지 확인
        assertThat(mockSession.getAttribute("signup")).isNull();
    }

    @Test
    @DisplayName("INHABITANT 전체 회원가입 플로우 통합 테스트")
    void completeInhabitantSignupFlow() throws Exception {
        // 1단계: 사용자 타입 설정
        mockMvc.perform(post("/signup/user-type")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userType": "INHABITANT"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("REGION"));

        // 2단계: 지역 설정
        mockMvc.perform(post("/signup/region")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"regionId": "11560"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("AGE"));

        // 3단계: 연령대 설정
        mockMvc.perform(post("/signup/age-range")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ageRange": "THIRTIES"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("GENDER"));

        // 4단계: 성별 설정 (INHABITANT는 바로 NICKNAME으로)
        mockMvc.perform(post("/signup/gender")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"gender": "FEMALE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("NINAME"));

        // 5단계: 닉네임 생성
        mockMvc.perform(post("/signup/nickname")
                        .session(mockSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isString());

        // 6단계: 회원가입 완료
        long userCountBefore = usersRepository.count();

        mockMvc.perform(post("/signup/complete")
                        .session(mockSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jwtAccessToken").isNotEmpty());

        long userCountAfter = usersRepository.count();
        assertThat(userCountAfter).isEqualTo(userCountBefore + 1);
    }

    @Test
    @DisplayName("잘못된 단계 순서로 진행 시 실패")
    void invalidStepOrder_ShouldFail() throws Exception {
        // 사용자 타입 설정 없이 지역 설정 시도
        mockMvc.perform(post("/signup/region")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"regionId": "11010"}
                                """))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("INHABITANT가 FOUNDER 전용 단계 접근 시 실패")
    void inhabitant_AccessingFounderOnlySteps_ShouldFail() throws Exception {
        // INHABITANT로 회원가입 시작
        mockMvc.perform(post("/signup/user-type")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userType": "INHABITANT"}
                                """))
                .andExpect(status().isOk());

        // 지역, 연령대, 성별 설정
        setupBasicSteps();

        // FOUNDER 전용 단계인 관심업종 설정 시도 (실패해야 함)
        mockMvc.perform(post("/signup/interests")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"interests": ["MANUFACTURING"]}
                                """))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("뒤로가기 기능 - 이전 단계 데이터 조회")
    void backwardNavigation_GetPreviousStepData() throws Exception {
        // FOUNDER로 관심업종까지 설정
        setupFounderToInterests();

        // 관심업종 데이터 조회
        mockMvc.perform(get("/signup/experience/data")
                        .session(mockSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.experience").exists());
    }

    @Test
    @DisplayName("유효하지 않은 enum 값 입력 시 실패")
    void invalidEnumValue_ShouldFail() throws Exception {
        mockMvc.perform(post("/signup/user-type")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userType": "INVALID_TYPE"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("필수 필드 누락 시 실패")
    void missingRequiredFields_ShouldFail() throws Exception {
        mockMvc.perform(post("/signup/user-type")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    private void initializeSession() {
        SignupSession signupSession = new SignupSession();
        signupSession.setUsername("testuser");
        signupSession.setEmail("test@example.com");
        signupSession.setProfileImageUrl("https://example.com/profile.jpg");
        mockSession.setAttribute("signup", signupSession);
    }

    private void setupBasicSteps() throws Exception {
        mockMvc.perform(post("/signup/region")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"regionId": "11010"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/signup/age-range")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ageRange": "TWENTIES"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/signup/gender")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"gender": "MALE"}
                                """))
                .andExpect(status().isOk());
    }

    private void setupFounderToInterests() throws Exception {
        mockMvc.perform(post("/signup/user-type")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userType": "FOUNDER"}
                                """))
                .andExpect(status().isOk());

        setupBasicSteps();

        mockMvc.perform(post("/signup/interests")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"interests": ["MANUFACTURING"]}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/signup/budget")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"budget": "THOUSANDS_3000_5000"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/signup/experience")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"experience": "NO"}
                                """))
                .andExpect(status().isOk());
    }
}