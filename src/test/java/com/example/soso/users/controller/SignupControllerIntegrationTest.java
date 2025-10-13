package com.example.soso.users.controller;

import com.example.soso.users.repository.UsersRepository;
import com.example.soso.users.domain.dto.SignupSession;
import com.example.soso.config.TestRedisConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.context.annotation.Import;
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
@Import(TestRedisConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
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
                .andExpect(jsonPath("$").value("NICKNAME"));

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
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user").exists())
                .andExpect(jsonPath("$.user.id").exists());

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
                .andExpect(jsonPath("$").value("NICKNAME"));

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
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user").exists())
                .andExpect(jsonPath("$.user.id").exists());

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
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("해당 사용자 유형에서 사용할 수 없는 단계입니다."));
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
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("해당 사용자 유형에서 사용할 수 없는 단계입니다."));
    }

    @Test
    @DisplayName("뒤로가기 후 순서대로 재진행 가능")
    void backwardNavigation_RevisitStepsSuccessfully() throws Exception {
        startFounderFlowUpToGender();

        // 뒤로가서 지역 재설정
        mockMvc.perform(post("/signup/region")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"regionId": "22020"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("AGE"));

        // 연령대 재설정
        mockMvc.perform(post("/signup/age-range")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ageRange": "THIRTIES"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("GENDER"));

        // 다시 성별 설정
        mockMvc.perform(post("/signup/gender")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"gender": "FEMALE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("INTERESTS"));
    }

    @Test
    @DisplayName("뒤로가기 후 단계 건너뛰면 실패하고 다음 단계 안내")
    void backwardNavigation_SkipStep_ShouldFail() throws Exception {
        startFounderFlowUpToGender();

        // 뒤로가서 지역 재설정
        mockMvc.perform(post("/signup/region")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"regionId": "33030"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("AGE"));

        // 연령대 재설정까지 완료
        mockMvc.perform(post("/signup/age-range")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ageRange": "TWENTIES"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("GENDER"));

        // 성별을 건너뛰고 관심사로 이동 시도 -> 실패
        mockMvc.perform(post("/signup/interests")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"interests": ["MANUFACTURING"]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("다음 단계: gender"));
    }

    @Test
    @DisplayName("여러 단계 이전으로 이동 후 다시 진행 가능")
    void backwardNavigation_MultipleStepsSuccess() throws Exception {
        startFounderFlowThroughBudget();

        // 나이대로 돌아가기
        mockMvc.perform(post("/signup/age-range")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ageRange": "FORTIES"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("GENDER"));

        // 성별 재설정
        mockMvc.perform(post("/signup/gender")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"gender": "MALE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("INTERESTS"));

        // 관심사, 예산, 경험 순으로 다시 진행
        mockMvc.perform(post("/signup/interests")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"interests": ["ACCOMMODATION_FOOD"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("BUDGET"));

        mockMvc.perform(post("/signup/budget")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"budget": "THOUSANDS_5000_7000"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("STARTUP"));
    }

    @Test
    @DisplayName("유형 변경 시 새로운 플로우를 따르도록 제한")
    void changeUserType_ShouldRestrictFounderOnlySteps() throws Exception {
        // 창업자로 진행 중 일부 단계 수행
        mockMvc.perform(post("/signup/user-type")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userType": "FOUNDER"}
                                """))
                .andExpect(status().isOk());

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

        // 주민으로 변경
        mockMvc.perform(post("/signup/user-type")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userType": "INHABITANT"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("REGION"));

        // 주민 플로우 단계 진행
        mockMvc.perform(post("/signup/region")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"regionId": "11560"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("AGE"));

        mockMvc.perform(post("/signup/age-range")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ageRange": "THIRTIES"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("GENDER"));

        mockMvc.perform(post("/signup/gender")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"gender": "MALE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("NICKNAME"));

        // 주민 상태에서 창업자 전용 단계 접근 시도
        mockMvc.perform(post("/signup/interests")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"interests": ["MANUFACTURING"]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("해당 사용자 유형에서 사용할 수 없는 단계입니다."));
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
        signupSession.setUsername("testUser");
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

    private void startFounderFlowUpToGender() throws Exception {
        mockMvc.perform(post("/signup/user-type")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userType": "FOUNDER"}
                                """))
                .andExpect(status().isOk());

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

    private void startFounderFlowThroughBudget() throws Exception {
        startFounderFlowUpToGender();

        mockMvc.perform(post("/signup/interests")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"interests": ["MANUFACTURING", "ACCOMMODATION_FOOD"]}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/signup/budget")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"budget": "THOUSANDS_3000_5000"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("STARTUP"));
    }
}
